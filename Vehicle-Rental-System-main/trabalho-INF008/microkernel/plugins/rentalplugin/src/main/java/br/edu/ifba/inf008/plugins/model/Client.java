package br.edu.ifba.inf008.plugins.model;

public class Client {

    private int id;
    private String email;

    public Client(String email) {
        this.email = email;
    }

    public Client(int id, String email) {
        this.id = id;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return email;
    }
}
