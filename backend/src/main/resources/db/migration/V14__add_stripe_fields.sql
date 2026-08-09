-- Añadir ID de cliente a la Persona (Pasajero)
ALTER TABLE persona ADD COLUMN stripe_customer_id VARCHAR(255) UNIQUE;
-- Añadir ID de cuenta conectada (Conductor)
ALTER TABLE persona ADD COLUMN stripe_account_id VARCHAR(255) UNIQUE;

-- Vincular el pago con la transacción real de Stripe
ALTER TABLE pago ADD COLUMN stripe_payment_intent_id VARCHAR(255) UNIQUE;