import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import * as FileSystem from 'expo-file-system';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { StatusBar } from 'expo-status-bar';

interface Download {
  id: string;
  filename: string;
  url: string;
  timestamp: number;
  size?: number;
  status: 'completed' | 'downloading' | 'failed';
  progress?: number;
  localUri?: string;
}

export default function DownloadsScreen() {
  const router = useRouter();
  const [downloads, setDownloads] = useState<Download[]>([]);
  const [activeDownloads, setActiveDownloads] = useState<Download[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDownloads();
    const interval = setInterval(loadDownloads, 2000); // Actualizar cada 2 segundos
    return () => clearInterval(interval);
  }, []);

  const loadDownloads = async () => {
    try {
      const downloadsJson = await AsyncStorage.getItem('downloads_history');
      const activeJson = await AsyncStorage.getItem('active_downloads');
      
      if (downloadsJson) {
        setDownloads(JSON.parse(downloadsJson));
      }
      
      if (activeJson) {
        setActiveDownloads(JSON.parse(activeJson));
      }
    } catch (error) {
      console.error('Error loading downloads:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatFileSize = (bytes?: number): string => {
    if (!bytes) return 'Desconocido';
    const mb = bytes / (1024 * 1024);
    if (mb < 1) {
      return `${(bytes / 1024).toFixed(2)} KB`;
    }
    return `${mb.toFixed(2)} MB`;
  };

  const formatDate = (timestamp: number): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (minutes < 1) return 'Ahora';
    if (minutes < 60) return `Hace ${minutes}m`;
    if (hours < 24) return `Hace ${hours}h`;
    return `Hace ${days}d`;
  };

  const openFile = async (download: Download) => {
    if (!download.localUri) {
      Alert.alert('Error', 'No se puede abrir el archivo');
      return;
    }

    try {
      const fileInfo = await FileSystem.getInfoAsync(download.localUri);
      if (!fileInfo.exists) {
        Alert.alert('Error', 'El archivo ya no existe');
        return;
      }

      // TODO: Implementar visualización según tipo de archivo
      Alert.alert(
        download.filename,
        `Tamaño: ${formatFileSize(fileInfo.size)}\nRuta: ${download.localUri}`,
        [
          { text: 'Cerrar', style: 'cancel' },
          {
            text: 'Eliminar',
            style: 'destructive',
            onPress: () => deleteFile(download),
          },
        ]
      );
    } catch (error) {
      Alert.alert('Error', 'No se pudo acceder al archivo');
    }
  };

  const deleteFile = async (download: Download) => {
    Alert.alert(
      'Eliminar archivo',
      `¿Deseas eliminar "${download.filename}"?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Eliminar',
          style: 'destructive',
          onPress: async () => {
            try {
              if (download.localUri) {
                await FileSystem.deleteAsync(download.localUri, { idempotent: true });
              }

              // Remover del historial
              const newDownloads = downloads.filter(d => d.id !== download.id);
              setDownloads(newDownloads);
              await AsyncStorage.setItem('downloads_history', JSON.stringify(newDownloads));

              Alert.alert('Éxito', 'Archivo eliminado correctamente');
            } catch (error) {
              Alert.alert('Error', 'No se pudo eliminar el archivo');
            }
          },
        },
      ]
    );
  };

  const clearAllDownloads = async () => {
    Alert.alert(
      'Limpiar historial',
      '¿Deseas eliminar todo el historial de descargas? Los archivos no se eliminarán.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Limpiar',
          style: 'destructive',
          onPress: async () => {
            try {
              await AsyncStorage.setItem('downloads_history', JSON.stringify([]));
              setDownloads([]);
              Alert.alert('Éxito', 'Historial limpiado');
            } catch (error) {
              Alert.alert('Error', 'No se pudo limpiar el historial');
            }
          },
        },
      ]
    );
  };

  const deleteAllFiles = async () => {
    Alert.alert(
      '⚠️ Eliminar todos los archivos',
      '¿Estás seguro? Esta acción no se puede deshacer.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Eliminar Todo',
          style: 'destructive',
          onPress: async () => {
            try {
              // Eliminar todos los archivos
              for (const download of downloads) {
                if (download.localUri) {
                  try {
                    await FileSystem.deleteAsync(download.localUri, { idempotent: true });
                  } catch (e) {
                    console.error('Error deleting file:', e);
                  }
                }
              }

              // Limpiar historial
              await AsyncStorage.setItem('downloads_history', JSON.stringify([]));
              setDownloads([]);

              Alert.alert('Éxito', 'Todos los archivos han sido eliminados');
            } catch (error) {
              Alert.alert('Error', 'Ocurrió un error al eliminar los archivos');
            }
          },
        },
      ]
    );
  };

  if (loading) {
    return (
      <View style={styles.container}>
        <StatusBar style="light" />
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <StatusBar style="light" />
      
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => router.back()}
        >
          <Ionicons name="arrow-back" size={24} color="#e2e8f0" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Descargas</Text>
        <TouchableOpacity
          style={styles.menuButton}
          onPress={() => {
            Alert.alert('Opciones', 'Selecciona una opción', [
              { text: 'Limpiar historial', onPress: clearAllDownloads },
              { text: 'Eliminar todos los archivos', onPress: deleteAllFiles, style: 'destructive' },
              { text: 'Cancelar', style: 'cancel' },
            ]);
          }}
        >
          <Ionicons name="ellipsis-vertical" size={24} color="#e2e8f0" />
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.scrollView}>
        {/* Descargas Activas */}
        {activeDownloads.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Descargando ahora</Text>
            {activeDownloads.map((download) => (
              <View key={download.id} style={styles.downloadItem}>
                <View style={styles.downloadIcon}>
                  <Ionicons name="cloud-download" size={24} color="#6366f1" />
                </View>
                <View style={styles.downloadInfo}>
                  <Text style={styles.downloadName} numberOfLines={1}>
                    {download.filename}
                  </Text>
                  <View style={styles.progressBar}>
                    <View
                      style={[
                        styles.progressFill,
                        { width: `${download.progress || 0}%` },
                      ]}
                    />
                  </View>
                  <Text style={styles.downloadSize}>
                    {download.progress || 0}% - {formatFileSize(download.size)}
                  </Text>
                </View>
              </View>
            ))}
          </View>
        )}

        {/* Historial de Descargas */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>
            Historial ({downloads.length})
          </Text>

          {downloads.length === 0 ? (
            <View style={styles.emptyState}>
              <Ionicons name="download-outline" size={64} color="#64748b" />
              <Text style={styles.emptyText}>No hay descargas</Text>
              <Text style={styles.emptySubtext}>
                Los archivos descargados aparecerán aquí
              </Text>
            </View>
          ) : (
            downloads.map((download) => (
              <TouchableOpacity
                key={download.id}
                style={styles.downloadItem}
                onPress={() => openFile(download)}
              >
                <View style={styles.downloadIcon}>
                  {download.status === 'completed' ? (
                    <Ionicons name="document" size={24} color="#10b981" />
                  ) : download.status === 'failed' ? (
                    <Ionicons name="alert-circle" size={24} color="#ef4444" />
                  ) : (
                    <Ionicons name="time" size={24} color="#f59e0b" />
                  )}
                </View>
                <View style={styles.downloadInfo}>
                  <Text style={styles.downloadName} numberOfLines={1}>
                    {download.filename}
                  </Text>
                  <View style={styles.downloadMeta}>
                    <Text style={styles.downloadSize}>
                      {formatFileSize(download.size)}
                    </Text>
                    <Text style={styles.downloadDot}>•</Text>
                    <Text style={styles.downloadTime}>
                      {formatDate(download.timestamp)}
                    </Text>
                  </View>
                </View>
                <TouchableOpacity
                  style={styles.deleteButton}
                  onPress={(e) => {
                    e.stopPropagation();
                    deleteFile(download);
                  }}
                >
                  <Ionicons name="trash-outline" size={20} color="#ef4444" />
                </TouchableOpacity>
              </TouchableOpacity>
            ))
          )}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingTop: 48,
    backgroundColor: '#1e293b',
    borderBottomWidth: 1,
    borderBottomColor: '#334155',
  },
  backButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#e2e8f0',
  },
  menuButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollView: {
    flex: 1,
  },
  section: {
    padding: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#e2e8f0',
    marginBottom: 12,
  },
  downloadItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#1e293b',
    borderRadius: 12,
    padding: 12,
    marginBottom: 8,
  },
  downloadIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#334155',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  downloadInfo: {
    flex: 1,
  },
  downloadName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#e2e8f0',
    marginBottom: 4,
  },
  downloadMeta: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  downloadSize: {
    fontSize: 14,
    color: '#94a3b8',
  },
  downloadDot: {
    fontSize: 14,
    color: '#94a3b8',
    marginHorizontal: 8,
  },
  downloadTime: {
    fontSize: 14,
    color: '#94a3b8',
  },
  deleteButton: {
    padding: 8,
  },
  progressBar: {
    width: '100%',
    height: 4,
    backgroundColor: '#334155',
    borderRadius: 2,
    marginVertical: 8,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#6366f1',
    borderRadius: 2,
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 64,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#e2e8f0',
    marginTop: 16,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#94a3b8',
    marginTop: 8,
  },
});
