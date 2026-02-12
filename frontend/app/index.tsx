import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function Index() {
  const router = useRouter();

  useEffect(() => {
    checkServerConfig();
  }, []);

  const checkServerConfig = async () => {
    try {
      const serverUrl = await AsyncStorage.getItem('SERVER_URL');
      
      if (serverUrl) {
        // Si ya hay una URL configurada, ir al WebView
        router.replace('/webview');
      } else {
        // Si no hay URL, ir a la configuración
        router.replace('/config');
      }
    } catch (error) {
      console.error('Error checking server config:', error);
      router.replace('/config');
    }
  };

  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#6366f1" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
    alignItems: 'center',
    justifyContent: 'center',
  },
});