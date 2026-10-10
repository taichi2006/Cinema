package com.cinema.product;

import com.cinema.common.dto.PageResponse;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

//GET /products : Danh sách sản phẩm F&B kèm query category, status, page, size.
@WebServlet(name = "ProductController", urlPatterns = {"/products", "/products/*"})
public class ProductController extends BaseServlet {

    private final ProductService service = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String category = req.getParameter("category");
        String status = req.getParameter("status");
        int page = getIntParam(req, "page", 0);
        int size = getIntParam(req, "size", 20);

        PageResponse<ProductResponse> pageData = service.getProducts(category, status, page, size);
        writeSuccess(resp, pageData);
    }
}
