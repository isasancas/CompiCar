package com.compicar.viaje;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ViajeServiceImpl implements ViajeService {

    private final ViajeRepository viajeRepository;

    public ViajeServiceImpl(ViajeRepository viajeRepository) {
        this.viajeRepository = viajeRepository;
    }
    
}
