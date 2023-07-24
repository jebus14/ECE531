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
        // If the URI matches the pattern "/[0-9]+", which is a slash followed by one or more digits
        if (uri.matches("/[0-9]+")) { 
            // Parse the ID from the URI (remove the leading slash and convert to an integer)
            int id = Integer.parseInt(uri.substring(1));

            // Fetch the data with the given ID from the database
            DataObject dataObject = connection.getData(id);

            // If the data exists, convert it to JSON and return it
            if (dataObject != null) {
                String json = gson.toJson(dataObject);
                return newFixedLengthResponse(Response.Status.OK, "application/json", json);
            } else {
                // If the data does not exist, return a 404 Not Found error
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Data not found");
            }
        }

        // If the URI is "/", return all data
        if (uri.equals("/")) {
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
        if (!uri.matches("/[0-9]+")) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid URI");
        }

        int id = Integer.parseInt(uri.substring(1)); // Parse the ID from the URI

        DataObject dataObject = extractDataObjectFromRequest(session);
        if (dataObject == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid data");
        }

        // Set the ID on the data object from the URI
        dataObject.setId(id);

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
