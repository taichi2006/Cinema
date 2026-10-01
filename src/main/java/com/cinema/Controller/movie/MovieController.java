package com.cinema.Controller;

import com.cinema.Model.DTO.MovieDTO;
import com.cinema.Model.Service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/movies")
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
            List<MovieDTO> movies =
                    movieService.getAllMovies();

            objectMapper.writeValue(
                    response.getWriter(),
                    movies
            );

        } catch (Exception exception) {

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                    "{\"error\":\"Cannot get movies\"}"
            );
        }
    }
}