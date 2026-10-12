package com.cinema.home.controller;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.home.service.HomeService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/home", "/home/*"})
public class HomeController extends BaseServlet {

    private final HomeService service = new HomeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // GET /home (Dữ liệu trang chủ)
        if (segments.length == 0) {
            writeSuccess(resp, service.getHomeData());
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
