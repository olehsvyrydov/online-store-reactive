package org.javaprojects.onlinestore.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler(WrongQuantityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalStateException(WrongQuantityException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "redirect:/items/" + e.getItemId();
    }

    // To simplify the code, we use the same page for all exceptions. Only for learning purposes.
    @ExceptionHandler({IllegalStateException.class, HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleIllegalArgumentException(Exception e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "404";
    }
}
