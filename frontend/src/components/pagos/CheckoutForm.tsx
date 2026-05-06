import React, { useState } from 'react';
import { PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { buildApiUrl } from '../../apiConfig';

interface CheckoutFormProps {
  clientSecret: string;
  onSuccess: (paymentIntentId: string) => void;
  monto: number;
}

const CheckoutForm: React.FC<CheckoutFormProps> = ({ clientSecret, onSuccess, monto }) => {
  const stripe = useStripe();
  const elements = useElements();
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setIsProcessing(true);

    // Confirmamos el pago con Stripe
    const { error, paymentIntent } = await stripe.confirmPayment({
      elements,
      redirect: 'if_required', // Evita redirecciones externas si es posible
    });

    if (error) {
      setErrorMessage(error.message || "Ocurrió un error inesperado.");
      setIsProcessing(false);
    } else if (paymentIntent && paymentIntent.status === 'succeeded') {
      // Si el pago es exitoso, notificamos al padre para que cree la reserva en Java
      onSuccess(paymentIntent.id);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <PaymentElement />
      {errorMessage && <div className="text-red-500 text-xs font-bold">{errorMessage}</div>}
      <button
        disabled={isProcessing || !stripe}
        className="w-full bg-gradient-compi text-white py-3 rounded-xl font-bold shadow-lg"
      >
        {isProcessing ? "Procesando pago..." : `Confirmar Pago de ${monto.toFixed(2)}€`}
      </button>
    </form>
  );
};

export default CheckoutForm;