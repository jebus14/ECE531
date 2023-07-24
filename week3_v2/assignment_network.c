// Jesus Sanchez
// ECE531


//1.  Comment out one of the URL options
//2.  Compile using make
//    -for gcc: make
//    -for arm: make -f makefile-arm
//3.  Startup simple HTTP Server: sudo python -m SimpleHTTPServer 


#include <stdio.h>
#include <string.h>
#include <curl/curl.h>

#define OK         0
#define INIT_ERR   1
#define REQ_ERR    2

//URL Option1: Use if running program on your host machine
#define URL              "http://localhost:8000" 

//URL Option 2: Set IP to your host machine IP adress if running on QEMU enviroment 
//#define URL              "<IP Adress>:8000" 

void print_help() {
    printf("Command: hw [options] [message]\n");
    printf("Options:\n");
    printf("  --url   <url>     Specify the URL to send the HTTP request\n");
    printf("  --post            Send an HTTP POST request\n");
    printf("  --get             Send an HTTP GET request\n");
    printf("  --put             Send an HTTP PUT request\n");
    printf("  --delete          Send an HTTP DELETE request\n");
    printf("  --help            Print this help message\n");
}

size_t write_callback(char *ptr, size_t size, size_t nmemb, void *userdata) {
    size_t total_size = size * nmemb;
    printf("%.*s", (int)total_size, ptr);
    return total_size;
}

int main(int argc, char *argv[]) {
    CURL *curl;
    CURLcode res;
    char *url = NULL;
    char *message = NULL;
    int request_type = 0;  // 1: POST, 2: GET, 3: PUT, 4: DELETE

    // Parse command line arguments
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--url") == 0) {
            if (i + 1 < argc) {
                url = argv[++i];
            } else {
                printf("Error: No URL specified after -u/--url\n");
                return INIT_ERR;
            }
        } else if (strcmp(argv[i], "--post") == 0) {
            request_type = 1;
        } else if (strcmp(argv[i], "--get") == 0) {
            request_type = 2;
        } else if (strcmp(argv[i], "--put") == 0) {
            request_type = 3;
        } else if (strcmp(argv[i], "--delete") == 0) {
            request_type = 4;
        } else if (strcmp(argv[i], "--help") == 0) {
            print_help();
            return OK;
        } else {
            message = argv[i];
        }
    }

    if (!url) {
        printf("Error: No URL specified. Use: hw --url <url> \n");
        return INIT_ERR;
    }

	curl = curl_easy_init();
	if (curl) {
		curl_easy_setopt(curl, CURLOPT_URL, URL);
		curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);

		if (request_type == 1) {  // POST
            if (!message) {
                printf("Error: No message.\n");
                return INIT_ERR;
            }
          
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, message);

        } 
        else if (request_type == 2) {  // GET
        	curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L);
        } 
        else if (request_type == 3) {  // PUT
            if (!message) {
                printf("Error: No message.\n");
                return INIT_ERR;
            }
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "PUT");
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, message);
        } 
        else if (request_type == 4) {  // DELETE
            curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, message);
        }

		res = curl_easy_perform(curl);
		if(res != CURLE_OK) {
			return REQ_ERR;
		}
		curl_easy_cleanup(curl);
	} else{
		return INIT_ERR;
	}
	return OK;
}
