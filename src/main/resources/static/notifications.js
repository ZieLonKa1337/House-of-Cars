// rstumm2s
(() => {
    if (Notification.permission == 'default') {
        Notification.requestPermission();
    }

    (function connect() {
        let socket = new WebSocket('ws://' + location.host + '/ws/status/notifications');
        socket.onclose = connect;
        socket.onmessage = msg => {
            let data = JSON.parse(msg.data);
            new Notification(data.title, {
                badge: location.host + '/favicon.ico',
                body: data.body
            });
        };
    })();
})();
