import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { StatusBar } from 'expo-status-bar';

export default function Config() {
  const router = useRouter();
  const [serverUrl, setServerUrl] = useState('http://192.168.43.101');
  const [streamingPort, setStreamingPort] = useState('3001');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    loadSavedConfig();
  }, []);

  const loadSavedConfig = async () => {
    try {
      const savedUrl = await AsyncStorage.getItem('SERVER_URL');
      const savedPort = await AsyncStorage.getItem('STREAMING_PORT');
      
      if (savedUrl) setServerUrl(savedUrl);
      if (savedPort) setStreamingPort(savedPort);
    } catch (error) {
      console.error('Error loading config:', error);
    }
  };

  const validateUrl = (url: string): boolean => {
    // Validación básica de URL
    const urlPattern = /^https?:\/\/.+/;
    return urlPattern.test(url);
  };

  const handleSave = async () => {
    if (!validateUrl(serverUrl)) {
      Alert.alert(
        'URL Inválida',
        'Por favor ingresa una URL válida (ejemplo: http://192.168.1.100)'
      );
      return;
    }

    if (!streamingPort || isNaN(Number(streamingPort))) {
      Alert.alert(
        'Puerto Inválido',
        'Por favor ingresa un puerto válido (ejemplo: 3001)'
      );
      return;
    }

    setIsLoading(true);

    try {
      await AsyncStorage.setItem('SERVER_URL', serverUrl);
      await AsyncStorage.setItem('STREAMING_PORT', streamingPort);

      Alert.alert(
        'Configuración Guardada',
        'La configuración se ha guardado correctamente',
        [
          {
            text: 'OK',
            onPress: () => router.replace('/webview'),
          },
        ]
      );
    } catch (error) {
      console.error('Error saving config:', error);
      Alert.alert('Error', 'No se pudo guardar la configuración');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <StatusBar style="light" />
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.header}>
          <View style={styles.logoContainer}>
            <View style={styles.logo}>
              <Text style={styles.logoText}>SP</Text>
            </View>
          </View>
          <Text style={styles.title}>StreamPay</Text>
          <Text style={styles.subtitle}>Configuración del Servidor</Text>
        </View>

        <View style={styles.form}>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>URL del Servidor</Text>
            <TextInput
              style={styles.input}
              value={serverUrl}
              onChangeText={setServerUrl}
              placeholder="http://192.168.1.100"
              placeholderTextColor="#64748b"
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
            />
            <Text style={styles.hint}>
              Ingresa la IP o dominio donde está alojado StreamPay
            </Text>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Puerto de Streaming</Text>
            <TextInput
              style={styles.input}
              value={streamingPort}
              onChangeText={setStreamingPort}
              placeholder="3001"
              placeholderTextColor="#64748b"
              keyboardType="number-pad"
            />
            <Text style={styles.hint}>
              Puerto del servicio de streaming de video
            </Text>
          </View>

          <TouchableOpacity
            style={[styles.button, isLoading && styles.buttonDisabled]}
            onPress={handleSave}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>
              {isLoading ? 'Guardando...' : 'Guardar y Continuar'}
            </Text>
          </TouchableOpacity>

          <View style={styles.infoBox}>
            <Text style={styles.infoTitle}>💡 Información</Text>
            <Text style={styles.infoText}>
              • Asegúrate de estar en la misma red que el servidor
            </Text>
            <Text style={styles.infoText}>
              • La URL debe comenzar con http:// o https://
            </Text>
            <Text style={styles.infoText}>
              • Puedes cambiar esta configuración en cualquier momento
            </Text>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  scrollContent: {
    flexGrow: 1,
    padding: 24,
    justifyContent: 'center',
  },
  header: {
    alignItems: 'center',
    marginBottom: 40,
  },
  logoContainer: {
    marginBottom: 16,
  },
  logo: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#6366f1',
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#ffffff',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#94a3b8',
  },
  form: {
    width: '100%',
  },
  inputGroup: {
    marginBottom: 24,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#e2e8f0',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#1e293b',
    borderWidth: 1,
    borderColor: '#334155',
    borderRadius: 12,
    padding: 16,
    fontSize: 16,
    color: '#ffffff',
  },
  hint: {
    fontSize: 14,
    color: '#64748b',
    marginTop: 8,
  },
  button: {
    backgroundColor: '#6366f1',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
  },
  infoBox: {
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 16,
    marginTop: 24,
    borderLeftWidth: 4,
    borderLeftColor: '#6366f1',
  },
  infoTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#e2e8f0',
    marginBottom: 12,
  },
  infoText: {
    fontSize: 14,
    color: '#94a3b8',
    marginBottom: 8,
    lineHeight: 20,
  },
});