package com.smartpark.backend.service;

import com.smartpark.backend.dto.PaymentDTO;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.model.Payment;

import java.util.List;

public interface IPaymentService {
    PaymentDTO calculateMonthlySalary (String p, int month, int year);
    PaymentDTO updatePaymentByDetails(String matricule, PaymentDTO paymentDTO);
    //void deletePayment(String matricule);
    PaymentDTO findByMatricule(String matricule);
    //List<Payment> generateMassPayments(int month, int year);
     void deleteByMatricule(String matricule);
     void deleteByMatriculeAndMonthAndYear(String matricule, int month, int year);
    PaymentDTO findSpecificPayment(String matricule, int month, int year);
}

