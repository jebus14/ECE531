//Jesus Sanchez
//ECE 531

// Compile with: gcc time_daemon.c -o time_daemon
// run file
// check /var/log/syslog




#include <sys/types.h>
#include <sys/stat.h>
#include <syslog.h>
#include <signal.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <time.h>

#define DAEMON_NAME "time_daemon"

#define OK 0
#define ERR_FORK 1
#define ERR_SETSID 2
#define ERR_CHDIR 3
#define ERR_WTF 4

static void _signal_handler(const int signal) {
    switch (signal) {
        case SIGHUP:
            break;
        case SIGTERM:
            syslog(LOG_INFO, "received SIGTERM, exiting.");
            closelog();
            exit(OK);
            break;
        default:
            syslog(LOG_INFO, "received unhandled signal");
    }
}

void _get_time(void) {
    for (int i = 0; i < 30; i++) {
        time_t current_time;
        time(&current_time);

        syslog(LOG_INFO, "Current time: %s", ctime(&current_time));
        sleep(1);
    }
    syslog(LOG_INFO, "Time_Daemon Stopping");

}

int main(void) {
    openlog(DAEMON_NAME, LOG_PID | LOG_NDELAY | LOG_NOWAIT, LOG_DAEMON);

    syslog(LOG_INFO, "starting time sampled");

    pid_t pid = fork();
    
    if (pid < 0) {
        syslog(LOG_ERR, "%s", strerror(errno));
        return ERR_FORK;
    }

    if (pid > 0) {
        return OK;
    }

    if (setsid() < -1) {
        syslog(LOG_ERR, "%s", strerror(errno));
        return ERR_SETSID;
    }

    close(STDIN_FILENO);
    close(STDOUT_FILENO);
    close(STDERR_FILENO);

    umask(S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);

    if (chdir("/") < 0) {
        syslog(LOG_ERR, "%s", strerror(errno));
        return ERR_CHDIR;
    }

    signal(SIGTERM, _signal_handler);
    signal(SIGHUP, _signal_handler);

    _get_time();

    return ERR_WTF;
}

