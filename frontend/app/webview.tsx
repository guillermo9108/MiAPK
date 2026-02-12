import React, { useRef, useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  BackHandler,
  Alert,
  TouchableOpacity,
  Text,
  PanResponder,
  Animated,
  Modal,
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

export default function WebViewScreen() {
  const webViewRef = useRef<WebView>(null);
  const router = useRouter();
  const [serverUrl, setServerUrl] = useState('');
  const [canGoBack, setCanGoBack] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [activeDownloads, setActiveDownloads] = useState<Download[]>([]);
  const [currentOrientation, setCurrentOrientation] = useState<ScreenOrientation.Orientation>(ScreenOrientation.Orientation.PORTRAIT_UP);
  const [isVideoPlaying, setIsVideoPlaying] = useState(false);
  
  // Estado para el FAB con swipe gesture
  const fabOpacity = useRef(new Animated.Value(0)).current;
  const [showFab, setShowFab] = useState(false);

  useEffect(() => {
    loadServerUrl();
    setupNotifications();
    loadActiveDownloads();
    setupOrientationListener();
    
    const backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      handleBackPress
    );

    // Configurar PanResponder para swipe desde el borde izquierdo
    return () => {
      backHandler.remove();
      ScreenOrientation.removeOrientationChangeListeners();
      ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.PORTRAIT).catch(() => {});
    };
  }, []);

  // Efecto para manejar cambios de orientación cuando hay video
  useEffect(() => {
    handleOrientationChange();
  }, [currentOrientation, isVideoPlaying]);

  const setupOrientationListener = async () => {
    // Desbloquear orientación para permitir rotación
    await ScreenOrientation.unlockAsync();
    
    // Listener para cambios de orientación
    ScreenOrientation.addOrientationChangeListener((event) => {
      setCurrentOrientation(event.orientationInfo.orientation);
    });
  };

  const handleOrientationChange = () => {
    const isLandscape = 
      currentOrientation === ScreenOrientation.Orientation.LANDSCAPE_LEFT ||
      currentOrientation === ScreenOrientation.Orientation.LANDSCAPE_RIGHT;
    
    const isPortrait = 
      currentOrientation === ScreenOrientation.Orientation.PORTRAIT_UP ||
      currentOrientation === ScreenOrientation.Orientation.PORTRAIT_DOWN;

    // Si hay video reproduciéndose y el usuario gira a landscape
    if (isVideoPlaying && isLandscape && !isFullscreen) {
      enterFullscreen();
    }
    
    // Si está en fullscreen y el usuario vuelve a portrait
    if (isFullscreen && isPortrait) {
      exitFullscreen();
    }
  };

  const enterFullscreen = () => {
    webViewRef.current?.injectJavaScript(`
      (function() {
        try {
          const videos = document.querySelectorAll('video');
          if (videos.length > 0) {
            const playingVideo = Array.from(videos).find(v => !v.paused);
            if (playingVideo) {
              if (playingVideo.requestFullscreen) {
                playingVideo.requestFullscreen();
              } else if (playingVideo.webkitRequestFullscreen) {
                playingVideo.webkitRequestFullscreen();
              } else if (playingVideo.webkitEnterFullscreen) {
                playingVideo.webkitEnterFullscreen();
              }
            }
          }
        } catch(e) {
          console.log('Error entering fullscreen:', e);
        }
      })();
      true;
    `);
  };

  // PanResponder para detectar swipe desde el borde izquierdo
  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: (evt, gestureState) => {
        // Solo activar si el toque empieza en los primeros 30px desde la izquierda
        return evt.nativeEvent.pageX < 30;
      },
      onMoveShouldSetPanResponder: (evt, gestureState) => {
        return evt.nativeEvent.pageX < 30 && gestureState.dx > 10;
      },
      onPanResponderMove: (evt, gestureState) => {
        if (gestureState.dx > 50 && !showFab) {
          toggleFab(true);
        }
      },
      onPanResponderRelease: () => {},
    })
  ).current;

  const toggleFab = (show: boolean) => {
    setShowFab(show);
    Animated.timing(fabOpacity, {
      toValue: show ? 1 : 0,
      duration: 200,
      useNativeDriver: true,
    }).start();
  };

  const loadActiveDownloads = async () => {
    try {
      const activeJson = await AsyncStorage.getItem('active_downloads');
      if (activeJson) {
        setActiveDownloads(JSON.parse(activeJson));
      }
    } catch (error) {
      console.error('Error loading active downloads:', error);
    }
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
    if (showMenu) {
      setShowMenu(false);
      return true;
    }
    if (showFab) {
      toggleFab(false);
      return true;
    }
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
      
      if (data.type === 'fullscreenchange') {
        setIsFullscreen(data.isFullscreen);
        if (data.isFullscreen) {
          await ScreenOrientation.unlockAsync();
          toggleFab(false); // Ocultar FAB en fullscreen
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
    const downloadId = Date.now().toString();
    const newDownload: Download = {
      id: downloadId,
      filename,
      url,
      timestamp: Date.now(),
      status: 'downloading',
      progress: 0,
    };

    try {
      // Agregar a descargas activas
      const updatedActive = [...activeDownloads, newDownload];
      setActiveDownloads(updatedActive);
      await AsyncStorage.setItem('active_downloads', JSON.stringify(updatedActive));

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
          
          // Actualizar progreso
          setActiveDownloads(prev => 
            prev.map(d => 
              d.id === downloadId 
                ? { ...d, progress: Math.round(progress * 100), size: downloadProgress.totalBytesExpectedToWrite }
                : d
            )
          );
        }
      );

      const result = await downloadResumable.downloadAsync();
      
      if (result) {
        // Completado exitosamente
        const completedDownload: Download = {
          ...newDownload,
          status: 'completed',
          localUri: result.uri,
          size: result.headers['Content-Length'] ? parseInt(result.headers['Content-Length']) : undefined,
          progress: 100,
        };

        // Remover de activas
        const newActive = activeDownloads.filter(d => d.id !== downloadId);
        setActiveDownloads(newActive);
        await AsyncStorage.setItem('active_downloads', JSON.stringify(newActive));

        // Agregar al historial
        const historyJson = await AsyncStorage.getItem('downloads_history');
        const history: Download[] = historyJson ? JSON.parse(historyJson) : [];
        history.unshift(completedDownload);
        await AsyncStorage.setItem('downloads_history', JSON.stringify(history));

        await Notifications.scheduleNotificationAsync({
          content: {
            title: '✅ Descarga completa',
            body: filename,
          },
          trigger: null,
        });
      }
    } catch (error) {
      // Error en descarga
      const newActive = activeDownloads.filter(d => d.id !== downloadId);
      setActiveDownloads(newActive);
      await AsyncStorage.setItem('active_downloads', JSON.stringify(newActive));

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
      '¿Deseas limpiar el caché del navegador?',
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
              webViewRef.current?.clearCache?.(true);
              await Notifications.dismissAllNotificationsAsync();
              
              Alert.alert('Éxito', 'Caché limpiado correctamente');
              webViewRef.current?.reload();
            } catch (error) {
              Alert.alert('Error', 'No se pudo limpiar el caché');
            }
            setShowMenu(false);
          },
        },
      ]
    );
  };

  const injectedJavaScript = `
    (function() {
      try {
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
    <View style={styles.container} {...panResponder.panHandlers}>
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
        userAgent="Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 StreamPayAPK/2.1"
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

      {/* FAB en esquina superior izquierda */}
      {showFab && !isFullscreen && (
        <Animated.View style={[styles.fabContainer, { opacity: fabOpacity }]}>
          <TouchableOpacity
            style={styles.fab}
            onPress={() => setShowMenu(!showMenu)}
          >
            <Ionicons name="menu" size={24} color="#ffffff" />
          </TouchableOpacity>
          
          {/* Badge de descargas activas */}
          {activeDownloads.length > 0 && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{activeDownloads.length}</Text>
            </View>
          )}
        </Animated.View>
      )}

      {/* Indicador de swipe */}
      {!showFab && !isFullscreen && (
        <View style={styles.swipeIndicator}>
          <Ionicons name="chevron-forward" size={16} color="#6366f1" />
        </View>
      )}

      {/* Menú Modal */}
      <Modal
        visible={showMenu}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setShowMenu(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowMenu(false)}
        >
          <View style={styles.menu}>
            <View style={styles.menuHeader}>
              <Text style={styles.menuTitle}>Menú</Text>
              <TouchableOpacity onPress={() => setShowMenu(false)}>
                <Ionicons name="close" size={24} color="#e2e8f0" />
              </TouchableOpacity>
            </View>

            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                router.push('/downloads');
              }}
            >
              <Ionicons name="download-outline" size={22} color="#e2e8f0" />
              <View style={styles.menuItemContent}>
                <Text style={styles.menuText}>Descargas</Text>
                {activeDownloads.length > 0 && (
                  <View style={styles.menuBadge}>
                    <Text style={styles.menuBadgeText}>{activeDownloads.length}</Text>
                  </View>
                )}
              </View>
            </TouchableOpacity>
            
            <View style={styles.menuDivider} />
            
            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                webViewRef.current?.reload();
              }}
            >
              <Ionicons name="refresh-outline" size={22} color="#e2e8f0" />
              <View style={styles.menuItemContent}>
                <Text style={styles.menuText}>Recargar</Text>
              </View>
            </TouchableOpacity>
            
            <View style={styles.menuDivider} />
            
            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                clearCache();
              }}
            >
              <Ionicons name="trash-outline" size={22} color="#e2e8f0" />
              <View style={styles.menuItemContent}>
                <Text style={styles.menuText}>Limpiar Caché</Text>
              </View>
            </TouchableOpacity>
            
            <View style={styles.menuDivider} />
            
            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                setShowMenu(false);
                router.push('/config');
              }}
            >
              <Ionicons name="settings-outline" size={22} color="#e2e8f0" />
              <View style={styles.menuItemContent}>
                <Text style={styles.menuText}>Configuración</Text>
              </View>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
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
  fabContainer: {
    position: 'absolute',
    top: 48,
    left: 16,
    zIndex: 1000,
  },
  fab: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#6366f1',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  badge: {
    position: 'absolute',
    top: -4,
    right: -4,
    backgroundColor: '#ef4444',
    borderRadius: 12,
    minWidth: 24,
    height: 24,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 6,
  },
  badgeText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
  },
  swipeIndicator: {
    position: 'absolute',
    left: 0,
    top: '50%',
    width: 24,
    height: 48,
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    borderTopRightRadius: 24,
    borderBottomRightRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: -24,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'flex-start',
    paddingTop: 100,
    paddingLeft: 16,
  },
  menu: {
    backgroundColor: '#1e293b',
    borderRadius: 16,
    marginRight: 16,
    maxWidth: 320,
    elevation: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.4,
    shadowRadius: 12,
    borderWidth: 1,
    borderColor: '#334155',
  },
  menuHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#334155',
  },
  menuTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#e2e8f0',
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 20,
  },
  menuItemContent: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginLeft: 16,
  },
  menuText: {
    color: '#e2e8f0',
    fontSize: 16,
  },
  menuBadge: {
    backgroundColor: '#ef4444',
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 6,
  },
  menuBadgeText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
  },
  menuDivider: {
    height: 1,
    backgroundColor: '#334155',
    marginHorizontal: 20,
  },
});
