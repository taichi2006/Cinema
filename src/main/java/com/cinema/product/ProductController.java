package com.cinema.product;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

//GET /products : Danh sách sản phẩm F&B kèm query category, status, page, size.
@WebServlet(name = "ProductController", urlPatterns = {"/products", "/products/*"})
public class ProductController extends HttpServlet {

    private final ProductService service = new ProductService();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String category = req.getParameter("category");
            String status = req.getParameter("status");
            int page = parseQueryInt(req.getParameter("page"), 0);
            int size = parseQueryInt(req.getParameter("size"), 20);

            var pageData = service.getProducts(category, status, page, size);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json;charset=UTF-8");
            json.writeValue(resp.getWriter(), ApiResponse.ok(pageData));

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    private int parseQueryInt(String val, int defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
