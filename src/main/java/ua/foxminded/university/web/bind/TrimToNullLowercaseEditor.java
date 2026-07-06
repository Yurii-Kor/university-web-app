package ua.foxminded.university.web.bind;

import java.beans.PropertyEditorSupport;
import java.util.Optional;

public class TrimToNullLowercaseEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        setValue(Optional.ofNullable(text)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .orElse(null));
    }
}