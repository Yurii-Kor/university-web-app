package ua.foxminded.university.web.bind;

import java.beans.PropertyEditorSupport;
import java.util.Optional;

public class TrimToNullUppercaseEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        setValue(Optional.ofNullable(text)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .orElse(null));
    }
}