package com.cinema.user.service;

import com.cinema.common.exception.ApiException;
import com.cinema.user.dao.UserDAO;
import com.cinema.user.dto.request.UpdateUserRequest;
import com.cinema.user.dto.response.UpdateProfileResponse;
import com.cinema.user.dto.response.UserProfileResponse;
import com.cinema.user.entity.User;

import java.util.Objects;
import java.util.regex.Pattern;

public class UserService {

    private static final int MIN_FULL_NAME_LENGTH = 2;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?[0-9]{9,15}|0[0-9]{9,10})$");

    private final UserDAO userDAO;

    public UserService() {
        this(new UserDAO());
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "UserDAO must not be null");
    }

    public UserProfileResponse getProfile(long userId) {
        try {
            User user = userDAO.findProfileById(userId)
                    .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));

            ensureActive(user);
            return mapUser(user);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.internal("Không thể tải hồ sơ người dùng");
        }
    }

    public UpdateProfileResponse updateProfile(long userId, UpdateUserRequest request) {
        UserDAO.UpdateProfileCommand command = validateAndCreateCommand(request);

        try {
            User currentUser = userDAO.findProfileById(userId)
                    .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));
            ensureActive(currentUser);

            User updatedUser = userDAO.updateProfile(userId, command)
                    .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));

            return new UpdateProfileResponse(updatedUser.getId(), updatedUser.getFullName());
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.internal("Không thể cập nhật hồ sơ người dùng");
        }
    }

    private UserDAO.UpdateProfileCommand validateAndCreateCommand(UpdateUserRequest request) {
        if (request == null || !request.hasAnyProvidedField()) {
            throw ApiException.badRequest("Phải cung cấp ít nhất một trường cần cập nhật");
        }

        String fullName = request.getFullName();
        if (request.wasFullNameProvided()) {
            if (fullName == null) {
                throw ApiException.badRequest("Họ tên không được để trống");
            }

            fullName = fullName.trim();
            int length = fullName.codePointCount(0, fullName.length());
            if (length < MIN_FULL_NAME_LENGTH || length > MAX_FULL_NAME_LENGTH) {
                throw ApiException.badRequest("Họ tên phải từ 2-100 ký tự");
            }
        }

        String phone = request.getPhone();
        if (request.wasPhoneProvided() && phone != null) {
            phone = phone.trim();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw ApiException.badRequest("Số điện thoại không hợp lệ");
            }
        }

        return new UserDAO.UpdateProfileCommand(
                request.wasFullNameProvided(),
                fullName,
                request.wasPhoneProvided(),
                phone,
                request.wasDobProvided(),
                request.getDob()
        );
    }

    private void ensureActive(User user) {
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw ApiException.forbidden("Tài khoản đã bị khóa");
        }
    }

    private UserProfileResponse mapUser(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus()
        );
    }
}
