import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { notificationsApi } from '../api/notificationsApi';
import { useWebSocket } from '../context/WebSocketContext';
import { useAuth } from '../context/AuthContext';
import {
  FiBell,
  FiCheck,
  FiCheckCircle,
  FiTrash2,
  FiAlertTriangle,
  FiInfo,
  FiClock,
  FiExternalLink,
  FiFilter,
  FiRefreshCw,
  FiX,
  FiArrowRight,
  FiShield,
  FiUserCheck
} from 'react-icons/fi';

export const formatRelativeTime = (timestamp) => {
  if (!timestamp) return '';
  try {
    const date = new Date(timestamp);
    const now = new Date();
    const diffSecs = Math.floor((now - date) / 1000);

    if (diffSecs < 30) return 'Just now';
    if (diffSecs < 60) return `${diffSecs}s ago`;
    const diffMins = Math.floor(diffSecs / 60);
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch {
    return '';
  }
};

export const getSeverityBadgeStyle = (severity) => {
  switch ((severity || '').toUpperCase()) {
    case 'CRITICAL':
      return {
        bg: '#fef2f2',
        color: '#dc2626',
        border: '#fca5a5',
        label: 'CRITICAL',
        icon: <FiAlertTriangle size={11} color="#dc2626" />
      };
    case 'HIGH':
      return {
        bg: '#fffbeb',
        color: '#d97706',
        border: '#fcd34d',
        label: 'HIGH',
        icon: <FiAlertTriangle size={11} color="#d97706" />
      };
    case 'MEDIUM':
      return {
        bg: '#eff6ff',
        color: '#2563eb',
        border: '#bfdbfe',
        label: 'MEDIUM',
        icon: <FiInfo size={11} color="#2563eb" />
      };
    case 'LOW':
    default:
      return {
        bg: '#f0fdf4',
        color: '#16a34a',
        border: '#bbf7d0',
        label: 'LOW',
        icon: <FiCheckCircle size={11} color="#16a34a" />
      };
  }
};

export default function NotificationCenter({
  isOpen = true,
  onClose = () => {},
  isStandalone = false,
  onUnreadCountChange = () => {},
  onCountChange
}) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { subscribeTopic } = useWebSocket();

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [criticalCount, setCriticalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState('ALL'); // ALL, INDIVIDUAL, UNREAD, CRITICAL, SLA, ASSIGNMENTS
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [selectedNotification, setSelectedNotification] = useState(null);

  const containerRef = useRef(null);

  // Fetch unread count
  const fetchCounts = useCallback(async () => {
    try {
      const res = await notificationsApi.getUnreadCount();
      const data = res?.data || res;
      if (data) {
        const count = data.unreadCount != null ? Number(data.unreadCount) : 0;
        const crit = data.criticalUnreadCount != null ? Number(data.criticalUnreadCount) : 0;
        setUnreadCount(count);
        setCriticalCount(crit);
        onUnreadCountChange?.(count, crit);
        onCountChange?.(count);
      }
    } catch (err) {
      console.warn('Failed to load notification counts:', err);
    }
  }, [onUnreadCountChange, onCountChange]);

  // Fetch notifications with active filter
  const fetchNotificationsList = useCallback(async (targetPage = 0, append = false) => {
    try {
      if (targetPage === 0 && !append) {
        setLoading(true);
      } else {
        setRefreshing(true);
      }
      setActionError(null);

      const params = {
        page: targetPage,
        size: 20
      };

      if (filter === 'INDIVIDUAL') {
        params.scope = 'INDIVIDUAL';
        params.directOnly = true;
      } else if (filter === 'UNREAD') {
        params.unreadOnly = true;
      } else if (filter === 'CRITICAL') {
        params.severity = 'CRITICAL';
      } else if (filter === 'SLA') {
        params.type = 'SLA';
      } else if (filter === 'ASSIGNMENTS') {
        params.type = 'INCIDENT_ASSIGNED';
      }

      const res = await notificationsApi.getNotifications(params);
      const pageData = res?.data || res;
      const content = pageData?.content || (Array.isArray(pageData) ? pageData : []);
      const totalPages = pageData?.totalPages || 1;

      if (append) {
        setNotifications((prev) => [...prev, ...content]);
      } else {
        setNotifications(content);
      }

      setPage(targetPage);
      setHasMore(targetPage + 1 < totalPages);
    } catch (err) {
      console.error('Failed to load notifications:', err);
      setActionError(err.message || 'Failed to fetch notifications');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [filter]);

  // Initial load & filter change
  useEffect(() => {
    fetchCounts();
    fetchNotificationsList(0, false);
  }, [fetchCounts, fetchNotificationsList]);

  // Real-time WebSocket subscription & Fallback 20-second polling
  useEffect(() => {
    // Single WebSocket topic subscription
    const unsubscribeWs = subscribeTopic('notifications', (incoming) => {
      if (!incoming || !incoming.id) return;

      // Ensure notification is addressed to current user or matches user role
      if (incoming.recipientUserId && user?.id && incoming.recipientUserId !== user.id) {
        return;
      }

      setNotifications((prev) => {
        // avoid duplicate if already present
        if (prev.some((n) => n.id === incoming.id)) {
          return prev;
        }

        // Check if matches active filter
        if (filter === 'INDIVIDUAL' && (!incoming.recipientUserId || incoming.recipientUserId !== user?.id)) {
          return prev;
        }
        if (filter === 'UNREAD' && (incoming.isRead || incoming.read || incoming.status === 'READ')) {
          return prev;
        }
        if (filter === 'CRITICAL' && incoming.severity !== 'CRITICAL') {
          return prev;
        }
        if (filter === 'SLA' && !String(incoming.notificationType || '').toUpperCase().includes('SLA')) {
          return prev;
        }
        if (filter === 'ASSIGNMENTS' && !String(incoming.notificationType || '').toUpperCase().includes('ASSIGN')) {
          return prev;
        }

        return [incoming, ...prev];
      });

      fetchCounts();
    });

    // Fallback database polling every 20 seconds
    const interval = setInterval(() => {
      fetchCounts();
      fetchNotificationsList(0, false);
    }, 20000);

    return () => {
      unsubscribeWs();
      clearInterval(interval);
    };
  }, [subscribeTopic, fetchCounts, fetchNotificationsList, filter, user]);

  // Single mark as read
  const handleMarkAsRead = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      const res = await notificationsApi.markAsRead(id);
      const updated = res?.data || res;
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === id ? { ...n, ...updated, isRead: true, read: true, status: 'READ', readAt: updated.readAt || new Date().toISOString() } : n
        )
      );
      setSelectedNotification((prev) =>
        prev && prev.id === id ? { ...prev, ...updated, isRead: true, read: true, status: 'READ', readAt: updated.readAt || new Date().toISOString() } : prev
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
      fetchCounts();
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
      setActionError('Could not mark as read');
    }
  };

  // Single mark as unread
  const handleMarkAsUnread = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      const res = await notificationsApi.markAsUnread(id);
      const updated = res?.data || res;
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === id ? { ...n, ...updated, isRead: false, read: false, status: 'UNREAD', readAt: null } : n
        )
      );
      setSelectedNotification((prev) =>
        prev && prev.id === id ? { ...prev, ...updated, isRead: false, read: false, status: 'UNREAD', readAt: null } : prev
      );
      setUnreadCount((prev) => prev + 1);
      fetchCounts();
    } catch (err) {
      console.error('Failed to mark notification as unread:', err);
      setActionError('Could not mark as unread');
    }
  };

  // Mark all as read
  const handleMarkAllAsRead = async () => {
    try {
      setRefreshing(true);
      await notificationsApi.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, status: 'READ', readAt: new Date().toISOString() }))
      );
      setSelectedNotification((prev) =>
        prev ? { ...prev, isRead: true, status: 'READ', readAt: new Date().toISOString() } : prev
      );
      setUnreadCount(0);
      setCriticalCount(0);
      onUnreadCountChange(0, 0);
    } catch (err) {
      console.error('Failed to mark all notifications as read:', err);
      setActionError('Could not mark all as read');
    } finally {
      setRefreshing(false);
    }
  };

  // Delete notification
  const handleDelete = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      await notificationsApi.deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      setSelectedNotification((prev) => (prev && prev.id === id ? null : prev));
      fetchCounts();
    } catch (err) {
      console.error('Failed to delete notification:', err);
      setActionError('Failed to remove notification');
    }
  };

  // Open notification details modal
  const handleItemClick = async (notification) => {
    const isUnread = notification.isRead !== true && notification.read !== true && notification.status !== 'READ';
    setSelectedNotification({
      ...notification,
      isRead: true,
      read: true,
      status: 'READ',
      readAt: notification.readAt || new Date().toISOString()
    });

    // If unread, mark as read in database
    if (isUnread) {
      handleMarkAsRead(notification.id);
    }

    // Refresh full details from backend to ensure latest state
    try {
      const res = await notificationsApi.getNotificationById(notification.id);
      const data = res?.data || res;
      if (data && data.id) {
        setSelectedNotification((prev) => ({
          ...data,
          isRead: isUnread ? true : (data.isRead ?? data.read ?? data.status === 'READ'),
          read: isUnread ? true : (data.read ?? data.isRead ?? data.status === 'READ'),
          status: isUnread ? 'READ' : (data.status || 'READ'),
        }));
      }
    } catch (err) {
      if (err.response?.status === 403) {
        setActionError('Access denied: You are not authorized to view this individual notification.');
        setSelectedNotification(null);
      }
    }
  };

  const handleLoadMore = () => {
    if (!loading && hasMore) {
      fetchNotificationsList(page + 1, true);
    }
  };

  if (!isOpen && !isStandalone) return null;

  return (
    <div
      ref={containerRef}
      className={`notification-center-root ${isStandalone ? 'standalone' : 'dropdown'}`}
    >
      {/* Header */}
      <div className="nc-header">
        <div className="nc-title-group">
          <div className="nc-icon-badge">
            <FiBell size={16} color="var(--accent-primary, #2563eb)" />
          </div>
          <div>
            <h3 className="nc-title">Notifications & Operational Alerts</h3>
            <span className="nc-subtitle">
              {unreadCount > 0 ? (
                <>
                  <strong style={{ color: 'var(--color-critical, #dc2626)' }}>{unreadCount} unread</strong>
                  {criticalCount > 0 && ` (${criticalCount} critical)`}
                </>
              ) : (
                'All caught up'
              )}
            </span>
          </div>
        </div>

        <div className="nc-actions">
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllAsRead}
              disabled={refreshing}
              className="btn btn-sm btn-ghost"
              title="Mark all notifications as read"
            >
              <FiCheckCircle size={13} />
              <span>Mark all read</span>
            </button>
          )}

          <button
            onClick={() => fetchNotificationsList(0, false)}
            disabled={loading || refreshing}
            className="btn btn-sm btn-ghost btn-icon-only"
            title="Refresh feed"
          >
            <FiRefreshCw size={13} className={refreshing ? 'spin' : ''} />
          </button>

          {!isStandalone && (
            <button
              onClick={onClose}
              className="btn btn-sm btn-ghost btn-icon-only"
              title="Close notifications"
            >
              <FiX size={15} />
            </button>
          )}
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="nc-filter-bar">
        <button
          className={`nc-filter-tab ${filter === 'ALL' ? 'active' : ''}`}
          onClick={() => setFilter('ALL')}
        >
          All
        </button>
        <button
          className={`nc-filter-tab ${filter === 'INDIVIDUAL' ? 'active' : ''}`}
          onClick={() => setFilter('INDIVIDUAL')}
        >
          Direct to Me
        </button>
        <button
          className={`nc-filter-tab ${filter === 'UNREAD' ? 'active' : ''}`}
          onClick={() => setFilter('UNREAD')}
        >
          Unread {unreadCount > 0 && <span className="nc-pill">{unreadCount}</span>}
        </button>
        <button
          className={`nc-filter-tab ${filter === 'CRITICAL' ? 'active' : ''}`}
          onClick={() => setFilter('CRITICAL')}
        >
          Critical {criticalCount > 0 && <span className="nc-pill critical">{criticalCount}</span>}
        </button>
        <button
          className={`nc-filter-tab ${filter === 'SLA' ? 'active' : ''}`}
          onClick={() => setFilter('SLA')}
        >
          SLA
        </button>
        <button
          className={`nc-filter-tab ${filter === 'ASSIGNMENTS' ? 'active' : ''}`}
          onClick={() => setFilter('ASSIGNMENTS')}
        >
          Assignments
        </button>
      </div>

      {/* Error alert banner */}
      {actionError && (
        <div className="nc-error-banner">
          <FiAlertTriangle size={13} />
          <span>{actionError}</span>
          <button onClick={() => setActionError(null)} className="nc-error-close">
            <FiX size={12} />
          </button>
        </div>
      )}

      {/* Notifications List */}
      <div className="nc-list-container">
        {loading && notifications.length === 0 ? (
          <div className="nc-loading-state">
            <div className="nc-spinner" />
            <p>Loading real-time notifications...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="nc-empty-state">
            <div className="nc-empty-icon">
              <FiBell size={28} />
            </div>
            <h4>No notifications found</h4>
            <p>
              {filter === 'UNREAD'
                ? "You have no unread notifications at this time."
                : filter === 'CRITICAL'
                ? 'No high-priority or critical alerts recorded.'
                : 'Operational events and SLA alerts will appear here.'}
            </p>
          </div>
        ) : (
          <div className="nc-items-list">
            {notifications.map((item) => {
              const isUnread = item.isRead !== true && item.read !== true && item.status !== 'READ';
              const sevBadge = getSeverityBadgeStyle(item.severity);

              return (
                <div
                  key={item.id}
                  onClick={() => handleItemClick(item)}
                  className={`nc-item ${isUnread ? 'unread' : 'read'} ${(item.incidentId || item.complaintId) ? 'clickable' : ''}`}
                >
                  <div className="nc-item-top">
                    <div className="nc-item-badges">
                      <span
                        className="nc-severity-badge"
                        style={{
                          backgroundColor: sevBadge.bg,
                          color: sevBadge.color,
                          borderColor: sevBadge.border
                        }}
                      >
                        {sevBadge.icon}
                        <span>{sevBadge.label}</span>
                      </span>

                      {item.notificationType && (
                        <span className="nc-type-badge">
                          {item.notificationType.replace(/_/g, ' ')}
                        </span>
                      )}

                      {isUnread && <span className="nc-unread-indicator" title="Unread" />}
                    </div>

                    <div className="nc-item-timestamp" title={item.createdAt}>
                      <FiClock size={11} />
                      <span>{formatRelativeTime(item.createdAt || item.sentAt)}</span>
                    </div>
                  </div>

                  <div className="nc-item-title">{item.title}</div>
                  <div className="nc-item-message">{item.message}</div>

                  <div className="nc-item-footer">
                    {item.complaintNumber || item.complaintId ? (
                      <span className="nc-incident-link">
                        <span>Grievance #{item.complaintNumber || item.complaintId.toString().slice(0, 8)}</span>
                        <FiArrowRight size={11} />
                      </span>
                    ) : item.incidentId ? (
                      <span className="nc-incident-link">
                        <span>Incident #{item.incidentId.toString().slice(0, 8)}</span>
                        <FiArrowRight size={11} />
                      </span>
                    ) : (
                      <span />
                    )}

                    <div className="nc-item-actions" onClick={(e) => e.stopPropagation()}>
                      {isUnread && (
                        <button
                          onClick={(e) => handleMarkAsRead(item.id, e)}
                          className="nc-action-btn"
                          title="Mark as read"
                        >
                          <FiCheck size={12} />
                          <span>Mark read</span>
                        </button>
                      )}

                      <button
                        onClick={(e) => handleDelete(item.id, e)}
                        className="nc-action-btn danger"
                        title="Delete notification"
                      >
                        <FiTrash2 size={12} />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}

            {hasMore && (
              <div className="nc-load-more">
                <button
                  onClick={handleLoadMore}
                  disabled={refreshing}
                  className="btn btn-sm btn-outline"
                >
                  {refreshing ? 'Loading more...' : 'Load previous notifications'}
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Footer link to dedicated page if in dropdown mode */}
      {!isStandalone && (
        <div className="nc-footer">
          <button
            onClick={() => {
              onClose();
              navigate('/notifications');
            }}
            className="nc-view-all-btn"
          >
            <span>Open Notification Center</span>
            <FiExternalLink size={12} />
          </button>
        </div>
      )}

      {/* Notification Details Modal */}
      {selectedNotification && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1200,
            padding: '1rem',
          }}
          onClick={() => setSelectedNotification(null)}
        >
          <div
            className="cc-modal"
            style={{
              width: '100%',
              maxWidth: '560px',
              backgroundColor: '#ffffff',
              borderRadius: '12px',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div
              style={{
                padding: '1.25rem 1.5rem',
                borderBottom: '1px solid var(--border-light, #e2e8f0)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                backgroundColor: '#f8fafc'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    backgroundColor: 'rgba(37, 99, 235, 0.1)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'var(--accent-primary, #2563eb)'
                  }}
                >
                  <FiBell size={16} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text-main, #0f172a)' }}>
                    Notification Details
                  </h3>
                  <span style={{ fontSize: '0.725rem', color: 'var(--text-muted, #64748b)' }}>
                    {formatRelativeTime(selectedNotification.createdAt || selectedNotification.sentAt)}
                  </span>
                </div>
              </div>

              <button
                onClick={() => setSelectedNotification(null)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-muted, #64748b)',
                  cursor: 'pointer',
                  padding: '4px',
                  borderRadius: '6px'
                }}
              >
                <FiX size={18} />
              </button>
            </div>

            {/* Modal Body */}
            <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem', maxHeight: '70vh', overflowY: 'auto' }}>
              {/* Badges Bar */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
                {(() => {
                  const sevBadge = getSeverityBadgeStyle(selectedNotification.severity);
                  return (
                    <span
                      style={{
                        padding: '3px 8px',
                        borderRadius: '9999px',
                        fontSize: '0.725rem',
                        fontWeight: 700,
                        backgroundColor: sevBadge.bg,
                        color: sevBadge.color,
                        border: `1px solid ${sevBadge.border}`,
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '4px'
                      }}
                    >
                      {sevBadge.icon}
                      {sevBadge.label}
                    </span>
                  );
                })()}

                {selectedNotification.notificationType && (
                  <span
                    style={{
                      padding: '3px 8px',
                      borderRadius: '9999px',
                      fontSize: '0.725rem',
                      fontWeight: 600,
                      backgroundColor: '#f1f5f9',
                      color: '#475569',
                      border: '1px solid #cbd5e1'
                    }}
                  >
                    {selectedNotification.notificationType.replace(/_/g, ' ')}
                  </span>
                )}

                <span
                  style={{
                    padding: '3px 8px',
                    borderRadius: '9999px',
                    fontSize: '0.725rem',
                    fontWeight: 600,
                    backgroundColor: (selectedNotification.isRead === true || selectedNotification.read === true || selectedNotification.status === 'READ') ? '#ecfdf5' : '#fef2f2',
                    color: (selectedNotification.isRead === true || selectedNotification.read === true || selectedNotification.status === 'READ') ? '#059669' : '#dc2626',
                    border: (selectedNotification.isRead === true || selectedNotification.read === true || selectedNotification.status === 'READ') ? '1px solid #a7f3d0' : '1px solid #fecaca'
                  }}
                >
                  {(selectedNotification.isRead === true || selectedNotification.read === true || selectedNotification.status === 'READ') ? '✓ READ' : '● UNREAD'}
                </span>

                {selectedNotification.channel && (
                  <span
                    style={{
                      padding: '3px 8px',
                      borderRadius: '9999px',
                      fontSize: '0.725rem',
                      color: '#64748b',
                      backgroundColor: '#f8fafc',
                      border: '1px solid #e2e8f0'
                    }}
                  >
                    Channel: {selectedNotification.channel}
                  </span>
                )}
              </div>

              {/* Title & Message */}
              <div>
                <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main, #0f172a)' }}>
                  {selectedNotification.title}
                </h4>
                <div
                  style={{
                    padding: '1rem',
                    backgroundColor: '#f8fafc',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '0.875rem',
                    lineHeight: 1.6,
                    color: '#334155',
                    whiteSpace: 'pre-wrap'
                  }}
                >
                  {selectedNotification.message}
                </div>
              </div>

              {/* Audience & Timestamp Metadata */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.775rem', color: '#64748b' }}>
                <div style={{ padding: '0.6rem 0.75rem', backgroundColor: '#f8fafc', borderRadius: '6px', border: '1px solid #f1f5f9' }}>
                  <span style={{ fontWeight: 600, display: 'block', color: '#475569', marginBottom: '2px' }}>Intended Audience:</span>
                  {selectedNotification.recipientUserId ? 'Direct Recipient (Individual)' : `Role Audience (${selectedNotification.recipientRole || 'All Staff'})`}
                </div>
                <div style={{ padding: '0.6rem 0.75rem', backgroundColor: '#f8fafc', borderRadius: '6px', border: '1px solid #f1f5f9' }}>
                  <span style={{ fontWeight: 600, display: 'block', color: '#475569', marginBottom: '2px' }}>Recorded At:</span>
                  {new Date(selectedNotification.createdAt || selectedNotification.sentAt).toLocaleString()}
                </div>
              </div>

              {/* Associated Grievance or Incident Card */}
              {(selectedNotification.complaintId || selectedNotification.complaintNumber || selectedNotification.incidentId) && (
                <div
                  style={{
                    padding: '0.85rem 1rem',
                    borderRadius: '8px',
                    backgroundColor: '#eff6ff',
                    border: '1px solid #bfdbfe',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between'
                  }}
                >
                  <div>
                    <span style={{ fontSize: '0.725rem', fontWeight: 600, color: '#1e40af', display: 'block' }}>
                      Associated Municipal Record
                    </span>
                    <span style={{ fontSize: '0.875rem', fontWeight: 700, color: '#1e3a8a' }}>
                      {selectedNotification.complaintNumber || (selectedNotification.complaintId ? `Grievance #${selectedNotification.complaintId.toString().slice(0, 8)}` : `Incident #${selectedNotification.incidentId.toString().slice(0, 8)}`)}
                    </span>
                  </div>

                  <button
                    onClick={() => {
                      const cId = selectedNotification.complaintId;
                      const iId = selectedNotification.incidentId;
                      setSelectedNotification(null);
                      onClose();
                      if (cId) {
                        navigate(`/complaints?id=${cId}`);
                      } else if (iId) {
                        navigate(`/incidents/${iId}`);
                      }
                    }}
                    className="btn btn-primary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '0.4rem 0.85rem', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                  >
                    <span>View Record</span>
                    <FiArrowRight size={12} />
                  </button>
                </div>
              )}
            </div>

            {/* Modal Footer Actions */}
            <div
              style={{
                padding: '1rem 1.5rem',
                borderTop: '1px solid var(--border-light, #e2e8f0)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                backgroundColor: '#ffffff'
              }}
            >
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                {(selectedNotification.isRead === true || selectedNotification.read === true || selectedNotification.status === 'READ') ? (
                  <button
                    onClick={(e) => handleMarkAsUnread(selectedNotification.id, e)}
                    className="btn btn-outline btn-sm"
                    style={{ fontSize: '0.775rem' }}
                  >
                    Mark as Unread
                  </button>
                ) : (
                  <button
                    onClick={(e) => handleMarkAsRead(selectedNotification.id, e)}
                    className="btn btn-primary btn-sm"
                    style={{ fontSize: '0.775rem' }}
                  >
                    Mark as Read
                  </button>
                )}

                <button
                  onClick={(e) => {
                    handleDelete(selectedNotification.id, e);
                    setSelectedNotification(null);
                  }}
                  className="btn btn-outline btn-sm"
                  style={{ fontSize: '0.775rem', color: '#dc2626', borderColor: '#fca5a5' }}
                >
                  <FiTrash2 size={12} style={{ marginRight: '4px' }} />
                  Delete
                </button>
              </div>

              <button
                onClick={() => setSelectedNotification(null)}
                className="btn btn-secondary btn-sm"
                style={{ fontSize: '0.775rem' }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
