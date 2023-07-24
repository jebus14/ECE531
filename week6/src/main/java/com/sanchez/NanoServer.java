package com.sanchez;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.sql.*;


import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class NanoServer extends NanoHTTPD {

    private DatabaseConnection connection;
    private Gson gson;

    public NanoServer() throws IOException {
        super(8080);
        connection = new DatabaseConnection();
        gson = new Gson();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning on http://localhost:8080/ \n");
    }

    public static void main(String[] args) {
        try {
            new NanoServer();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> headers = session.getHeaders();

        switch (method) {
            case GET:
                return handleGetRequest(uri);
            case POST:
                return handlePostOrPutRequest(uri, session, Method.POST);
            case PUT:
                return handlePostOrPutRequest(uri, session, Method.PUT);
            case DELETE:
                return handleDeleteRequest(uri);
            default:
                return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed");
        }
    }

    private Response handleGetRequest(String uri) {
        // extract id from the uri
        int id = extractIdFromUri(uri);

        // get the data object from the database
        DataObject dataObject = connection.getData(id);
        if (dataObject == null) {
            // no data found or an error occurred
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Data not found");
        }

        // convert the data object to json
        String json = gson.toJson(dataObject);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response handlePostOrPutRequest(String uri, IHTTPSession session, Method method) {
        // get the request body
        Map<String, String> body = new HashMap<String, String>();
        try {
            session.parseBody(body);
        } catch (IOException | ResponseException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid request");
        }

        // convert the request body to a data object
        DataObject dataObject = gson.fromJson(body.get("postData"), DataObject.class);
        if (dataObject == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid data");
        }

        if (method == Method.PUT) {
            // update the data
            boolean updated = connection.updateData(dataObject);
            return updated ? newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
                    : newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to update data");
        } else {
            // create new data
            int id = connection.createData(dataObject);
            return id > 0 ? newFixedLengthResponse(Response.Status.CREATED, MIME_PLAINTEXT, Integer.toString(id))
                    : newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to create data");
        }
    }

    private Response handleDeleteRequest(String uri) {
        // extract id from the uri
        int id = extractIdFromUri(uri);

        // delete the data
        boolean deleted = connection.deleteData(id);
        return deleted ? newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
                : newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to delete data");
    }

    private int extractIdFromUri(String uri) {
        // assuming the id is the last segment in the uri
        String[] segments = uri.split("/");
        return Integer.parseInt(segments[segments.length - 1]);
    }
}