package com.example.photobackend;

public enum BookingStatus {
    NEW("Новая"),
    IN_PROGRESS("В обработке"),
    COMPLETED("Готово"),
    CANCELLED("Отменена");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BookingStatus fromString(String text) {
        if (text == null) return NEW;
        String clean = text.replace("\"", "").trim();
        for (BookingStatus status : values()) {
            if (status.name().equalsIgnoreCase(clean) || status.displayName.equalsIgnoreCase(clean)) {
                return status;
            }
        }
        return NEW;
    }
}
