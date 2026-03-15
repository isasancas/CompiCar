package com.compicar.checkin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;

    @Autowired
    public CheckinServiceImpl(CheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }
    
}
