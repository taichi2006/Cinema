package com.cinema.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public final class UpdateUserRequest {
    private String fullName;
    private String phone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private boolean fullNameProvided;
    private boolean phoneProvided;
    private boolean dateOfBirthProvided;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.fullNameProvided = true;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.phoneProvided = true;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.dateOfBirthProvided = true;
    }

    public boolean wasFullNameProvided() {
        return fullNameProvided;
    }

    public boolean wasPhoneProvided() {
        return phoneProvided;
    }

    public boolean wasDateOfBirthProvided() {
        return dateOfBirthProvided;
    }

    public boolean hasAnyProvidedField() {
        return fullNameProvided || phoneProvided || dateOfBirthProvided;
    }
}
