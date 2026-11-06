# Propiedades opcionales de Stripe Connect

El código incluye valores predeterminados para estas propiedades, así que no es necesario agregarlas para arrancar en local.

Si necesitas personalizar país o URLs, agrega las líneas a `src/main/resources/application-local.properties` (archivo local ignorado por Git):

```properties
stripe.connect.country=MX
stripe.connect.refresh-url=http://localhost:8080/propietario?stripeOnboarding=refresh
stripe.connect.return-url=http://localhost:8080/propietario?stripeOnboarding=return
```

No reemplaces el archivo local completo ni compartas claves `sk_test_...`, secretos `whsec_...` o contraseñas.
