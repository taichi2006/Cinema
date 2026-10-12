package com.cinema.product.controller;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.product.service.ProductService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/products", "/products/*"})
public class ProductController extends BaseServlet {

    private final ProductService service = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // GET /products (Danh sách sản phẩm F&B)
        if (segments.length == 0) {
            String category = req.getParameter("category");
            String status = req.getParameter("status");
            int page = getIntParam(req, "page", 0);
            int size = getIntParam(req, "size", 20);

            writeSuccess(resp, service.getProducts(category, status, page, size));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
