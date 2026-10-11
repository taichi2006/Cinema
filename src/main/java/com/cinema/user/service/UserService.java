package com.cinema.user.service;

import com.cinema.common.exception.ApiException;
import com.cinema.user.dao.UserDAO;
import com.cinema.user.dto.request.UpdateUserRequest;
import com.cinema.user.dto.response.UpdateProfileResponse;
import com.cinema.user.dto.response.UserProfileResponse;
import com.cinema.user.entity.User;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    // 1. Lấy thông tin cá nhân (Profile)
    public UserProfileResponse getProfile(long userId) {
        User user = userDAO.findProfileById(userId)
                .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw ApiException.forbidden("Tài khoản đã bị khóa");
        }

        return new UserProfileResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getStatus()
        );
    }

    // 2. Cập nhật thông tin cá nhân
    public UpdateProfileResponse updateProfile(long userId, UpdateUserRequest request) {
        if (request == null || !request.hasAnyProvidedField()) {
            throw ApiException.badRequest("Phải cung cấp ít nhất một trường cần cập nhật");
        }

        String fullName = request.getFullName();
        if (request.wasFullNameProvided()) {
            if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
                throw ApiException.badRequest("Họ tên phải từ 2-100 ký tự");
            }
            fullName = fullName.trim();
        }

        String phone = request.getPhone();
        if (request.wasPhoneProvided() && phone != null) {
            phone = phone.trim();
            if (!phone.matches("^(\\+?[0-9]{9,15}|0[0-9]{9,10})$")) {
                throw ApiException.badRequest("Số điện thoại không hợp lệ");
            }
        }

        User currentUser = userDAO.findProfileById(userId)
                .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));
        if (!"ACTIVE".equalsIgnoreCase(currentUser.getStatus())) {
            throw ApiException.forbidden("Tài khoản đã bị khóa");
        }

        UserDAO.UpdateProfileCommand cmd = new UserDAO.UpdateProfileCommand(
                request.wasFullNameProvided(), fullName,
                request.wasPhoneProvided(), phone,
                request.wasDobProvided(), request.getDob()
        );

        User updatedUser = userDAO.updateProfile(userId, cmd)
                .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));

        return new UpdateProfileResponse(updatedUser.getId(), updatedUser.getFullName());
    }
}
