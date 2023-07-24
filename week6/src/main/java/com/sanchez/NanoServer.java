package com.sanchez;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
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
                return handlePostRequest(uri, session);
            case PUT:
                return handlePutRequest(uri, session);
            case DELETE:
                return handleDeleteRequest(uri);
            default:
                return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed");
        }
    }


    private Response handleGetRequest(String uri) {
        // if uri ends with "/", return all data
        if (uri.endsWith("/")) {
            List<DataObject> dataObjects = connection.getAllData();
            if (dataObjects.isEmpty()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "No data found");
            }
            String json = gson.toJson(dataObjects);
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
        // If it doesn't match, return a bad request response or handle the case appropriately
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid request");
    }

    private Response handlePostRequest(String uri, IHTTPSession session) {
        DataObject dataObject = extractDataObjectFromRequest(session);
        
        if (dataObject == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid data");
        }
        
        // create new data
        int id = connection.createData(dataObject);
        return id > 0 ? newFixedLengthResponse(Response.Status.CREATED, MIME_PLAINTEXT, "Record created successfully with ID: " + id)
                : newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to create data");
    }

    private Response handlePutRequest(String uri, IHTTPSession session) {
        DataObject dataObject = extractDataObjectFromRequest(session);
        
        if (dataObject == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid data");
        }

        // update the data
        boolean updated = connection.updateData(dataObject);
        return updated ? newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "Record updated successfully")
                : newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to update data");
    }

    private DataObject extractDataObjectFromRequest(IHTTPSession session) {
        // get the request body
        Map<String, String> body = new HashMap<String, String>();
        try {
            session.parseBody(body);
        } catch (IOException | ResponseException e) {
            return null;
        }

        // convert the request body to a data object
        return gson.fromJson(body.get("postData"), DataObject.class);
    }

    private Response handleDeleteRequest(String uri) {
        // extract id from the uri
        int id = extractIdFromUri(uri);

        // delete the data
        boolean deleted = connection.deleteData(id);
        return deleted ? newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "Record Deleted successfully")
                : newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to delete data");
    }

    private int extractIdFromUri(String uri) {
        String[] segments = uri.split("/");
        if (segments.length == 0) {
            // return a default value or throw an exception
            return -1;  // or throw new IllegalArgumentException("No ID in URI");
        }
        return Integer.parseInt(segments[segments.length - 1]);
    }
}
