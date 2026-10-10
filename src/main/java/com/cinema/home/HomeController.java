package com.cinema.home;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns = {"/home", "/home/*"})
public class HomeController extends BaseServlet {

    private final HomeService homeService = new HomeService();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            Map<String, Object> result = homeService.getHomeData();
            writeSuccess(response, result);

    }
}
