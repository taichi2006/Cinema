package com.cinema.user.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public final class UpdateUserRequest {
    private String fullName;
    private String phone;

    @JsonAlias({"dob", "dateOfBirth"})
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private boolean fullNameProvided;
    private boolean phoneProvided;
    private boolean dobProvided;

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

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
        this.dobProvided = true;
    }

    // Alias for dateOfBirth
    public LocalDate getDateOfBirth() {
        return dob;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dob = dateOfBirth;
        this.dobProvided = true;
    }

    public boolean wasFullNameProvided() {
        return fullNameProvided;
    }

    public boolean wasPhoneProvided() {
        return phoneProvided;
    }

    public boolean wasDateOfBirthProvided() {
        return dobProvided;
    }

    public boolean wasDobProvided() {
        return dobProvided;
    }

    public boolean hasAnyProvidedField() {
        return fullNameProvided || phoneProvided || dobProvided;
    }
}
