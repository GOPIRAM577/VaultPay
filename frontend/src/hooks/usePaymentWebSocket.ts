import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '../store/useAuthStore';

export type PaymentStatus =
  | 'INITIATED'
  | 'FRAUD_REVIEW'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'REJECTED';

interface UsePaymentWebSocketOptions {
  paymentId: string | null;
  onStatusUpdate: (status: PaymentStatus) => void;
}

/**
 * usePaymentWebSocket
 *
 * Opens a STOMP-over-WebSocket connection to the backend after a payment is
 * initiated and subscribes to /topic/payments/{paymentId} for live status
 * updates.
 *
 * Returns:
 *  - connected: whether the WebSocket handshake has completed
 *  - disconnect: function to manually close the connection
 */
export const usePaymentWebSocket = ({
  paymentId,
  onStatusUpdate,
}: UsePaymentWebSocketOptions) => {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const retryCount = useRef(0);
  const expectedClose = useRef(false);
  const token = useAuthStore.getState().token;
  const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const WS_URL = API_URL.replace(/^http/, 'ws');

  useEffect(() => {
    if (!paymentId) return;

    retryCount.current = 0;
    expectedClose.current = false;

    const connectClient = () => {
      const client = new Client({
        // Use native WebSocket without SockJS
        webSocketFactory: () => new WebSocket(`${WS_URL}/ws`),
        connectHeaders: {
          Authorization: token ? `Bearer ${token}` : '',
        },
        reconnectDelay: 0, // Disable automatic STOMP reconnect
        onConnect: () => {
          setConnected(true);
          client.subscribe(`/topic/payments/${paymentId}`, (message) => {
            try {
              const body = JSON.parse(message.body);
              if (body.status) {
                const status = body.status as PaymentStatus;
                onStatusUpdate(status);

                if (['COMPLETED', 'FAILED', 'REJECTED'].includes(status)) {
                  expectedClose.current = true;
                  client.deactivate();
                }
              }
            } catch (e) {
              console.error('Failed to parse WS message', e);
            }
          });
        },
        onDisconnect: () => setConnected(false),
        onWebSocketClose: () => {
          setConnected(false);
          if (!expectedClose.current && retryCount.current < 1) {
            retryCount.current++;
            console.log('WebSocket connection lost. Attempting 1-time reconnect in 2 seconds...');
            setTimeout(() => {
              if (!expectedClose.current) {
                connectClient();
              }
            }, 2000);
          } else {
            console.log('WebSocket reconnect aborted or max attempts reached.');
          }
        },
        onStompError: (frame) => {
          console.error('STOMP error', frame);
          setConnected(false);
        },
      });

      client.activate();
      clientRef.current = client;
    };

    connectClient();

    return () => {
      expectedClose.current = true;
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
      setConnected(false);
    };
  }, [paymentId]);

  const disconnect = () => {
    expectedClose.current = true;
    clientRef.current?.deactivate();
    setConnected(false);
  };

  return { connected, disconnect };
};
