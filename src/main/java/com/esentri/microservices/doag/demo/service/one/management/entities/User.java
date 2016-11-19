package com.esentri.microservices.doag.demo.service.one.management.entities;

import java.io.Serializable;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;

	private String name;

	private String password;

	public User(long id, String name, String password) {
		this.setId(id);
		this.setName(name);
		this.setPassword(password);
	}

	public User(String name, String password) {
		this.setName(name);
		this.setPassword(password);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

}
