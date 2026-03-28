package com.compicar.pago;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;

    @Autowired
    public PagoServiceImpl(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }
    
}
