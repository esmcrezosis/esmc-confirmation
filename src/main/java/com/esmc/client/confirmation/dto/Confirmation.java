package com.esmc.client.confirmation.dto;

import java.io.Serializable;


public class Confirmation implements Serializable{
	private static final long serialVersionUID =1L;
	private String codeMembre;
    private String stringTemplate;
    private String stringCardTemplate;
    private int procuration;
		
	public Confirmation() {
		
	}

	public String getCodeMembre() {
		return codeMembre;
	}

	public void setCodeMembre(String codeMembre) {
		this.codeMembre = codeMembre;
	}

	public String getStringTemplate() {
		return stringTemplate;
	}

	public void setStringTemplate(String stringTemplate) {
		this.stringTemplate = stringTemplate;
	}

	public String getStringCardTemplate() {
		return stringCardTemplate;
	}

	public void setStringCardTemplate(String stringCardTemplate) {
		this.stringCardTemplate = stringCardTemplate;
	}

	public int getProcuration() {
		return procuration;
	}

	public void setProcuration(int procuration) {
		this.procuration = procuration;
	}

	
}
