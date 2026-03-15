package com.compicar.parada;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParadaServiceImpl implements ParadaService {

    private final ParadaRepository paradaRepository;

    @Autowired
    public ParadaServiceImpl(ParadaRepository paradaRepository) {
        this.paradaRepository = paradaRepository;
    }
    
}
