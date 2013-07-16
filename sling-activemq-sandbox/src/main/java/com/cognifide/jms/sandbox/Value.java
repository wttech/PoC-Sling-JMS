package com.cognifide.jms.sandbox;

import java.io.Serializable;
import java.util.UUID;

public class Value implements Serializable {
	private static final long serialVersionUID = 2697394843844952456L;

	private final String uuid;

	public Value() {
		uuid = UUID.randomUUID().toString();
	}

	@Override
	public String toString() {
		return uuid;
	}
}