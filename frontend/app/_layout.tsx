import { Stack } from 'expo-router';
import { useEffect } from 'react';
import * as SystemUI from 'expo-system-ui';

export default function RootLayout() {
  useEffect(() => {
    // Establecer el color de la barra de estado y navegación
    SystemUI.setBackgroundColorAsync('#0f172a');
  }, []);

  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: '#0f172a' },
        animation: 'fade',
      }}
    >
      <Stack.Screen name="index" />
      <Stack.Screen name="config" />
      <Stack.Screen name="webview" />
    </Stack>
  );
}