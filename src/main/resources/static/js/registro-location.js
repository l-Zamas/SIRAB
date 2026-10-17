const button = document.querySelector("#usarUbicacion");
const reference = document.querySelector("#direccionReferencia");
const status = document.querySelector("#estadoUbicacion");

button?.addEventListener("click", () => {
    if (!navigator.geolocation) {
        status.textContent = "Este dispositivo no permite obtener la ubicación.";
        return;
    }

    status.textContent = "Solicitando permiso para obtener la ubicación…";
    navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
            reference.value = `Lat ${coords.latitude.toFixed(5)}, lon ${coords.longitude.toFixed(5)}`;
            status.textContent = "Ubicación añadida. Puedes editarla antes de continuar.";
        },
        () => {
            status.textContent = "No se pudo obtener la ubicación. Puedes escribirla manualmente.";
        },
        { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 }
    );
});
