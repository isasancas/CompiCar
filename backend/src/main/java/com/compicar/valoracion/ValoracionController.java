package com.compicar.valoracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/valoraciones")
public class ValoracionController {

    private final ValoracionService valoracionService;

    @Autowired
    public ValoracionController(ValoracionService valoracionService) {
        this.valoracionService = valoracionService;
    }
    
}
