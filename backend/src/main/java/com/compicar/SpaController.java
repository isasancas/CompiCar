package com.compicar;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
    // Esto captura cualquier ruta que NO sea un archivo (que no tenga punto)
    // y se la entrega a React para que él decida qué mostrar.
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirect() {
        return "forward:/index.html";
    }
}
