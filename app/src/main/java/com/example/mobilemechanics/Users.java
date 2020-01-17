package com.example.mobilemechanics;

class User {
    String name, password, number, address;


    public User(){

    }

    public User(String name, String password, String number, String address) {
        this.name = name;
        this.password = password;
        this.address = address;
        this.number  = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
