import React, { useRef, useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  BackHandler,
  Alert,
  TouchableOpacity,
  Text,
} from 'react-native';
import { WebView } from 'react-native-webview';
import { useRouter } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { StatusBar } from 'expo-status-bar';
import * as ScreenOrientation from 'expo-screen-orientation';
import * as Notifications from 'expo-notifications';
import * as FileSystem from 'expo-file-system';
import { Ionicons } from '@expo/vector-icons';

// Configurar notificaciones
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

export default function WebViewScreen() {
  const webViewRef = useRef<WebView>(null);
  const router = useRouter();
  const [serverUrl, setServerUrl] = useState('');
  const [canGoBack, setCanGoBack] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [showFab, setShowFab] = useState(false);
  const hideTimeout = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    loadServerUrl();
    setupNotifications();
    
    const backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      handleBackPress
    );

    return () => {
      backHandler.remove();
      if (hideTimeout.current) {
        clearTimeout(hideTimeout.current);
      }
      ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.PORTRAIT).catch(() => {});
    };
  }, [canGoBack]);

  const showFabTemporarily = () => {
    setShowFab(true);
    
    if (hideTimeout.current) {
      clearTimeout(hideTimeout.current);
    }
    
    hideTimeout.current = setTimeout(() => {
      setShowFab(false);
      setShowMenu(false);
    }, 3000);
  };

  const setupNotifications = async () => {
    try {
      await Notifications.requestPermissionsAsync();
    } catch (error) {
      console.log('Notifications permission error:', error);
    }
  };

  const loadServerUrl = async () => {
    try {
      const url = await AsyncStorage.getItem('SERVER_URL');
      if (url) {
        setServerUrl(url);
      } else {
        router.replace('/config');
      }
    } catch (error) {
      console.error('Error loading server URL:', error);
      router.replace('/config');
    }
  };

  const handleBackPress = () => {
    if (isFullscreen) {
      exitFullscreen();
      return true;
    }
    if (canGoBack && webViewRef.current) {
      webViewRef.current.goBack();
      return true;
    }
    return false;
  };

  const exitFullscreen = () => {
    webViewRef.current?.injectJavaScript(`
      try {
        if (document.fullscreenElement) {
          document.exitFullscreen();
        } else if (document.webkitFullscreenElement) {
          document.webkitExitFullscreen();
        }
      } catch(e) {}
      true;
    `);
  };

  const handleNavigationStateChange = (navState: any) => {
    setCanGoBack(navState.canGoBack);
  };

  const handleMessage = async (event: any) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      
      if (data.type === 'userInteraction') {
        showFabTemporarily();
      }
      
      if (data.type === 'fullscreenchange') {
        setIsFullscreen(data.isFullscreen);
        if (data.isFullscreen) {
          await ScreenOrientation.unlockAsync();
        } else {
          await ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.PORTRAIT);
        }
      }
      
      if (data.type === 'download') {
        handleDownload(data.url, data.filename);
      }
      
      if (data.type === 'audio') {
        if (data.action === 'playing') {
          showAudioNotification(data.title, data.artist);
        } else if (data.action === 'paused') {
          await Notifications.dismissAllNotificationsAsync();
        }
      }
    } catch (error) {
      // Ignorar errores de parsing
    }
  };

  const handleDownload = async (url: string, filename: string) => {
    try {
      await Notifications.scheduleNotificationAsync({
        content: {
          title: '📥 Descargando',
          body: filename,
        },
        trigger: null,
      });

      const downloadResumable = FileSystem.createDownloadResumable(
        url,
        FileSystem.documentDirectory + filename,
        {},
        (downloadProgress) => {
          const progress =
            downloadProgress.totalBytesWritten /
            downloadProgress.totalBytesExpectedToWrite;
          
          if (progress > 0 && progress < 1) {
            Notifications.scheduleNotificationAsync({
              content: {
                title: '📥 Descargando',
                body: `${filename} - ${Math.round(progress * 100)}%`,
              },
              trigger: null,
            });
          }
        }
      );

      const result = await downloadResumable.downloadAsync();
      
      if (result) {
        await Notifications.scheduleNotificationAsync({
          content: {
            title: '✅ Descarga completa',
            body: filename,
          },
          trigger: null,
        });
      }
    } catch (error) {
      await Notifications.scheduleNotificationAsync({
        content: {
          title: '❌ Error en descarga',
          body: filename,
        },
        trigger: null,
      });
    }
  };

  const showAudioNotification = async (title: string, artist: string) => {
    try {
      await Notifications.scheduleNotificationAsync({
        content: {
          title: '🎵 Reproduciendo',
          body: `${title}${artist ? ` - ${artist}` : ''}`,
          sound: false,
          priority: Notifications.AndroidNotificationPriority.HIGH,
          sticky: true,
        },
        trigger: null,
      });
    } catch (error) {
      console.log('Audio notification error:', error);
    }
  };

  const clearCache = async () => {
    Alert.alert(
      'Limpiar Caché',
      '¿Deseas limpiar el caché y el historial de descargas?',
      [
        {
          text: 'Cancelar',
          style: 'cancel',
        },
        {
          text: 'Limpiar',
          style: 'destructive',
          onPress: async () => {
            try {
              // Limpiar caché del WebView
              webViewRef.current?.clearCache?.(true);
              
              // Limpiar archivos descargados
              const downloadDir = FileSystem.documentDirectory;
              if (downloadDir) {
                const files = await FileSystem.readDirectoryAsync(downloadDir);
                for (const file of files) {
                  try {
                    await FileSystem.deleteAsync(downloadDir + file, { idempotent: true });
                  } catch (e) {}
                }
              }
              
              // Limpiar notificaciones
              await Notifications.dismissAllNotificationsAsync();
              
              Alert.alert('Éxito', 'Caché limpiado correctamente');
              
              // Recargar página
              webViewRef.current?.reload();
            } catch (error) {
              Alert.alert('Error', 'No se pudo limpiar el caché completamente');
            }
            setShowMenu(false);
          },
        },
      ]
    );
  };

  // JavaScript simplificado y seguro
  const injectedJavaScript = `
    (function() {
      try {
        let interactionTimeout;
        const notifyInteraction = () => {
          clearTimeout(interactionTimeout);
          interactionTimeout = setTimeout(() => {
            window.ReactNativeWebView.postMessage(JSON.stringify({
              type: 'userInteraction'
            }));
          }, 100);
        };

        let lastScroll = 0;
        window.addEventListener('scroll', () => {
          const currentScroll = window.pageYOffset || 0;
          if (Math.abs(currentScroll - lastScroll) > 50) {
            notifyInteraction();
            lastScroll = currentScroll;
          }
        }, { passive: true });

        document.addEventListener('touchstart', notifyInteraction, { passive: true });

        document.addEventListener('fullscreenchange', function() {
          window.ReactNativeWebView.postMessage(JSON.stringify({
            type: 'fullscreenchange',
            isFullscreen: !!document.fullscreenElement
          }));
        });
        
        document.addEventListener('webkitfullscreenchange', function() {
          window.ReactNativeWebView.postMessage(JSON.stringify({
            type: 'fullscreenchange',
            isFullscreen: !!document.webkitFullscreenElement
          }));
        });

        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
          video.addEventListener('dblclick', function() {
            if (this.requestFullscreen) {
              this.requestFullscreen();
            } else if (this.webkitRequestFullscreen) {
              this.webkitRequestFullscreen();
            }
          });
        });
      } catch(e) {
        console.log('Injection error:', e);
      }
    })();
    true;
  `;

  if (!serverUrl) {
    return <View style={styles.container} />;
  }

  return (
    <View style={styles.container}>
      <StatusBar style="light" hidden={isFullscreen} />
      
      <WebView
        ref={webViewRef}
        source={{ uri: serverUrl }}
        style={styles.webview}
        onNavigationStateChange={handleNavigationStateChange}
        onMessage={handleMessage}
        injectedJavaScript={injectedJavaScript}
        onError={(syntheticEvent) => {
          const { nativeEvent } = syntheticEvent;
          console.warn('WebView error: ', nativeEvent);
        }}
        onHttpError={(syntheticEvent) => {
          const { nativeEvent } = syntheticEvent;
          console.warn('HTTP error: ', nativeEvent);
        }}
        javaScriptEnabled={true}
        domStorageEnabled={true}
        allowFileAccess={true}
        mediaPlaybackRequiresUserAction={false}
        allowsFullscreenVideo={true}
        allowsInlineMediaPlayback={true}
        mixedContentMode="always"
        userAgent="Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 StreamPayAPK/2.1"
        cacheEnabled={true}
        cacheMode="LOAD_CACHE_ELSE_NETWORK"
        incognito={false}
        androidLayerType="hardware"
        androidHardwareAccelerationDisabled={false}
        setSupportMultipleWindows={false}
        startInLoadingState={true}
        renderLoading={() => (
          <View style={styles.loadingContainer}>
            <Text style={styles.loadingText}>Cargando StreamPay...</Text>
          </View>
        )}
      />

      {showFab && !isFullscreen && (
        <TouchableOpacity
          style={styles.fab}
          onPress={() => {
            setShowMenu(!showMenu);
            if (hideTimeout.current) {
              clearTimeout(hideTimeout.current);
            }
          }}
        >
          <Ionicons name="ellipsis-vertical" size={24} color="#ffffff" />
        </TouchableOpacity>
      )}

      {showMenu && !isFullscreen && (
        <>
          <TouchableOpacity
            style={styles.menuOverlay}
            activeOpacity={1}
            onPress={() => setShowMenu(false)}
          />
          
          <View style={styles.menu}>
            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                webViewRef.current?.reload();
              }}
            >
              <Ionicons name="refresh-outline" size={20} color="#e2e8f0" />
              <Text style={styles.menuText}>Recargar</Text>
            </TouchableOpacity>
            
            <View style={styles.menuDivider} />
            
            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                clearCache();
              }}
            >
              <Ionicons name="trash-outline" size={20} color="#e2e8f0" />
              <Text style={styles.menuText}>Limpiar Caché</Text>
            </TouchableOpacity>
            
            <View style={styles.menuDivider} />
            
            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                router.push('/config');
              }}
            >
              <Ionicons name="settings-outline" size={20} color="#e2e8f0" />
              <Text style={styles.menuText}>Configuración</Text>
            </TouchableOpacity>
          </View>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  webview: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  loadingContainer: {
    flex: 1,
    backgroundColor: '#0f172a',
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingText: {
    color: '#6366f1',
    fontSize: 16,
  },
  menuOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'transparent',
  },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 16,
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#6366f1',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  menu: {
    position: 'absolute',
    bottom: 80,
    right: 16,
    backgroundColor: '#1e293b',
    borderRadius: 12,
    paddingVertical: 8,
    minWidth: 180,
    elevation: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.4,
    shadowRadius: 12,
    borderWidth: 1,
    borderColor: '#334155',
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  menuText: {
    color: '#e2e8f0',
    fontSize: 16,
    marginLeft: 12,
  },
  menuDivider: {
    height: 1,
    backgroundColor: '#334155',
    marginVertical: 4,
  },
});