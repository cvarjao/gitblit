package com.gitblit.markup;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dagger.ObjectGraph;

public interface PagesController {
	public void init(ObjectGraph dagger);
	public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}
