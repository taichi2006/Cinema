package com.cinema.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/movie")
public class MovieController extends HttpServlet {

    private final MovieService movieService = new MovieService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        try {
            String q = request.getParameter("q");
            String genre = request.getParameter("genre");
            String status = request.getParameter("status");
            String sort = request.getParameter("sort");
            String pageStr = request.getParameter("page");
            String sizeStr = request.getParameter("size");

            int page = 1;
            if (pageStr != null && !pageStr.trim().isEmpty()) {
                try {
                    page = Integer.parseInt(pageStr.trim());
                } catch (NumberFormatException exception) {
                    throw new InvalidFilterException("Tham số 'page' phải là một số nguyên hợp lệ.");
                }
            }

            int size = 10;
            if (sizeStr != null && !sizeStr.trim().isEmpty()) {
                try {
                    size = Integer.parseInt(sizeStr.trim());
                } catch (NumberFormatException exception) {
                    throw new InvalidFilterException("Tham số 'size' phải là một số nguyên hợp lệ.");
                }
            }

            MovieFilter filter = new MovieFilter(q, genre, status, page, size, sort);
            SuccessEnvelope<List<MovieSummary>> result = movieService.getMovies(filter);

            response.setStatus(HttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getWriter(), result);

        } catch (InvalidFilterException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse("INVALID_FILTER", exception.getMessage())
            );
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(
                    response.getWriter(),
                    new ErrorResponse("INTERNAL_SERVER_ERROR", "Đã có lỗi xảy ra trên hệ thống.")
            );
        }
    }
}
