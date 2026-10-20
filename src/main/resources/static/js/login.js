document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector(".login-form");
    const password = document.querySelector("#password");
    const passwordToggle = document.querySelector(".login-password-toggle");
    const submitButton = document.querySelector(".login-submit");

    if (!(form instanceof HTMLFormElement)
            || !(password instanceof HTMLInputElement)
            || !(passwordToggle instanceof HTMLButtonElement)
            || !(submitButton instanceof HTMLButtonElement)) {
        throw new Error("No se encontraron los controles esperados del formulario de acceso.");
    }

    passwordToggle.addEventListener("click", () => {
        const shouldShowPassword = password.type === "password";
        password.type = shouldShowPassword ? "text" : "password";
        passwordToggle.setAttribute("aria-pressed", String(shouldShowPassword));
        passwordToggle.setAttribute(
            "aria-label",
            shouldShowPassword ? "Ocultar contraseña" : "Mostrar contraseña"
        );
        passwordToggle.textContent = shouldShowPassword ? "Ocultar" : "Mostrar";
    });

    form.addEventListener("submit", () => {
        submitButton.setAttribute("aria-busy", "true");
        submitButton.disabled = true;
    });
});
