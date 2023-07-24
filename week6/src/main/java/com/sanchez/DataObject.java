package com.sanchez;

public class DataObject {

    private int id;
    private String testWord;
    private int testNumber;

    public DataObject() {
    }

    public DataObject(int id, String testWord, int testNumber) {
        this.id = id;
        this.testWord = testWord;
        this.testNumber = testNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTestWord() {
        return testWord;
    }

    public void setTestWord(String testWord) {
        this.testWord = testWord;
    }

    public int getTestNumber() {
        return testNumber;
    }

    public void setTestNumber(int testNumber) {
        this.testNumber = testNumber;
    }
}