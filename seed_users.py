# -*- coding: utf-8 -*-
"""
seed_users.py — Furent Database Seeder
=======================================
Inserta 10,000 usuarios de prueba en la colección 'usuarios' de MongoDB
respetando todas las reglas de negocio y seguridad de la aplicación Furent.

Reglas aplicadas:
  • Dominios permitidos: gmail.com | outlook.com | outlook.es |
                         hotmail.com | hotmail.es | live.com
  • Contraseña: BCrypt (cost 10) — mínimo 8 chars, cumple ≥3/5 reglas
  • Índice único compuesto: (tenantId, email)
  • Role: USER (95%) | ADMIN (0%) — no se crean admins duplicados
  • Campos: igual al modelo com.alquiler.furent.model.User
  • Duplicados: se omiten automáticamente (upsert por email)

Dependencias (instalar una sola vez):
  pip install pymongo bcrypt faker tqdm

Uso:
  python seed_users.py
  python seed_users.py --total 5000
  python seed_users.py --host mongodb://localhost:27017 --db FurentDataBase
"""

import sys
# Forzar UTF-8 en la salida estandar (necesario en Windows con CP1252)
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

import argparse
import random
import secrets
import string
import uuid
from datetime import datetime, timedelta

# ── Dependencias opcionales con mensaje amigable ──────────────────────────────
try:
    import bcrypt
except ImportError:
    sys.exit("❌  Instala la dependencia: pip install bcrypt")

try:
    from pymongo import MongoClient, ASCENDING, errors
except ImportError:
    sys.exit("❌  Instala la dependencia: pip install pymongo")

try:
    from faker import Faker
except ImportError:
    sys.exit("❌  Instala la dependencia: pip install faker")

try:
    from tqdm import tqdm
except ImportError:
    sys.exit("❌  Instala la dependencia: pip install tqdm")

# ─────────────────────────────────────────────────────────────────────────────
# Configuración
# ─────────────────────────────────────────────────────────────────────────────
ALLOWED_DOMAINS   = ["gmail.com", "outlook.com", "outlook.es",
                     "hotmail.com", "hotmail.es", "live.com"]
TENANT_ID         = "default"
BCRYPT_COST       = 10          # igual que Spring Security default
BATCH_SIZE        = 500         # documentos por inserción a MongoDB
DEFAULT_TOTAL     = 10_000


# ─────────────────────────────────────────────────────────────────────────────
# Generador de contraseñas seguras
# ─────────────────────────────────────────────────────────────────────────────
UPPER   = string.ascii_uppercase
LOWER   = string.ascii_lowercase
DIGITS  = string.digits
SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?"

def _generate_password() -> str:
    """
    Produce una contraseña que cumple ≥3/5 de las reglas de Furent:
      1. ≥8 caracteres    ✅ siempre
      2. Mayúscula        ✅ siempre
      3. Minúscula        ✅ siempre
      4. Dígito           ✅ siempre
      5. Especial         ✅ siempre
    Score = 5/5 → contraseña de nivel "Excelente".
    """
    length = random.randint(10, 16)
    guaranteed = [
        secrets.choice(UPPER),
        secrets.choice(LOWER),
        secrets.choice(DIGITS),
        secrets.choice(SPECIAL),
    ]
    pool = UPPER + LOWER + DIGITS + SPECIAL
    rest = [secrets.choice(pool) for _ in range(length - len(guaranteed))]
    combined = guaranteed + rest
    random.shuffle(combined)
    return "".join(combined)

def _hash_password(plain: str) -> str:
    return bcrypt.hashpw(plain.encode(), bcrypt.gensalt(rounds=BCRYPT_COST)).decode()


# ─────────────────────────────────────────────────────────────────────────────
# Generador de documentos de usuario
# ─────────────────────────────────────────────────────────────────────────────
LANGUAGES   = ["es", "en", "pt", "fr", "de", "it"]
CURRENCIES  = ["COP", "USD", "EUR", "MXN", "ARS", "BRL", "CLP", "PEN"]
APPEARANCES = ["light", "dark"]

def _random_phone() -> str:
    """Número colombiano de 10 dígitos."""
    prefixes = ["300", "301", "302", "304", "305", "310", "311",
                "312", "313", "314", "315", "316", "317", "318",
                "319", "320", "321", "322", "323", "324", "350"]
    return random.choice(prefixes) + "".join([str(random.randint(0, 9)) for _ in range(7)])


def build_user(fake: Faker, used_emails: set) -> dict | None:
    """
    Intenta construir un documento de usuario único.
    Devuelve None si no pudo generar un email único en 10 intentos.
    """
    for _ in range(10):
        first  = fake.first_name()
        last   = fake.last_name()
        domain = random.choice(ALLOWED_DOMAINS)

        # Variantes de email: nombre.apellido, nombreXXX, n.apellidoXXX, etc.
        variant = random.randint(0, 4)
        suffix  = str(random.randint(1, 9999))
        if variant == 0:
            local = f"{first.lower()}.{last.lower()}{suffix}"
        elif variant == 1:
            local = f"{first.lower()}{suffix}"
        elif variant == 2:
            local = f"{first[0].lower()}{last.lower()}{suffix}"
        elif variant == 3:
            local = f"{last.lower()}.{first.lower()}{suffix}"
        else:
            local = f"{first.lower()}_{last.lower()}"

        # Limpiar caracteres inválidos en email local
        local = (local.replace(" ", "")
                      .replace("'", "")
                      .replace("á","a").replace("é","e").replace("í","i")
                      .replace("ó","o").replace("ú","u").replace("ñ","n")
                      .replace("ü","u"))
        email = f"{local}@{domain}"

        if email not in used_emails:
            used_emails.add(email)

            plain_pwd    = _generate_password()
            hashed_pwd   = _hash_password(plain_pwd)
            created_at   = datetime.utcnow() - timedelta(days=random.randint(0, 730))
            is_suspended = random.random() < 0.02   # 2% de usuarios suspendidos

            doc = {
                # No se incluye _id → MongoDB lo genera automáticamente
                "tenantId"          : TENANT_ID,
                "email"             : email,
                "password"          : hashed_pwd,
                "nombre"            : first,
                "apellido"          : last,
                "telefono"          : _random_phone(),
                "role"              : "USER",
                "fechaCreacion"     : created_at,
                "activo"            : not is_suspended,
                "razonSuspension"   : ("Violación de términos" if is_suspended else None),
                "fechaInicioSuspension" : (created_at if is_suspended else None),
                "fechaFinSuspension"    : (
                    created_at + timedelta(days=random.randint(7, 90))
                    if is_suspended and random.random() < 0.5
                    else None
                ),
                "suspensionPermanente"  : (is_suspended and random.random() < 0.1),
                "favoritos"             : [],
                "idioma"                : random.choice(LANGUAGES),
                "moneda"                : random.choice(CURRENCIES),
                "apariencia"            : random.choice(APPEARANCES),
                "notificacionesEmail"   : random.random() < 0.8,
                "totpSecret"            : None,
                "totpEnabled"           : False,
            }
            return doc

    return None   # no se pudo generar un email único


# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Furent — Seed de usuarios")
    parser.add_argument("--total",  type=int, default=DEFAULT_TOTAL,
                        help=f"Cantidad de usuarios a insertar (default: {DEFAULT_TOTAL})")
    parser.add_argument("--host",   default="mongodb://localhost:27017",
                        help="URI de MongoDB (default: mongodb://localhost:27017)")
    parser.add_argument("--db",     default="FurentDataBase",
                        help="Nombre de la base de datos (default: FurentDataBase)")
    parser.add_argument("--locale", default="es_CO",
                        help="Locale de Faker (default: es_CO)")
    args = parser.parse_args()

    print(f"\n{'═'*60}")
    print(f"  Furent — Seed de usuarios")
    print(f"{'═'*60}")
    print(f"  Host     : {args.host}")
    print(f"  Base datos: {args.db}")
    print(f"  Colección : usuarios")
    print(f"  Total     : {args.total:,}")
    print(f"  Locale    : {args.locale}")
    print(f"{'═'*60}\n")

    # Conexión
    client = MongoClient(args.host, serverSelectionTimeoutMS=5_000)
    try:
        client.admin.command("ping")
        print("✅  Conectado a MongoDB\n")
    except Exception as e:
        sys.exit(f"❌  No se pudo conectar a MongoDB: {e}")

    db         = client[args.db]
    collection = db["usuarios"]

    # Asegurar índice único compuesto (tenantId + email) — igual que Spring
    collection.create_index(
        [("tenantId", ASCENDING), ("email", ASCENDING)],
        unique=True,
        name="tenant_email_idx",
    )
    # Índice único en email solo
    try:
        collection.create_index("email", unique=True, name="email_1")
    except errors.OperationFailure:
        pass  # puede ya existir con otro nombre

    # Emails ya existentes para evitar duplicados locales
    print("📋  Cargando emails existentes en la BD...", end=" ", flush=True)
    used_emails: set = {doc["email"] for doc in collection.find({}, {"email": 1})}
    print(f"{len(used_emails):,} encontrados\n")

    fake          = Faker(args.locale)
    Faker.seed(42)

    inserted_total  = 0
    skipped_total   = 0
    batch           = []

    print(f"⚙️   Generando y hashing contraseñas (BCrypt cost={BCRYPT_COST})...")
    print(f"     Esto puede tardar ~{args.total // 100} segundos — por favor espera...\n")

    with tqdm(total=args.total, unit="usuario", colour="green",
              bar_format="{l_bar}{bar}| {n_fmt}/{total_fmt} [{elapsed}<{remaining}, {rate_fmt}]") as pbar:

        while inserted_total + len(batch) < args.total:
            doc = build_user(fake, used_emails)
            if doc is None:
                skipped_total += 1
                continue

            batch.append(doc)

            if len(batch) >= BATCH_SIZE:
                n = _flush_batch(collection, batch)
                inserted_total += n
                pbar.update(n)
                batch.clear()

        # Último batch parcial
        if batch:
            n = _flush_batch(collection, batch)
            inserted_total += n
            pbar.update(n)

    print(f"\n{'═'*60}")
    print(f"  ✅  Insertados : {inserted_total:,}")
    print(f"  ⚠️   Omitidos   : {skipped_total:,}  (email duplicado o colisión)")
    print(f"  📦  Total en BD: {collection.count_documents({}):,}")
    print(f"{'═'*60}\n")
    client.close()


def _flush_batch(collection, batch: list) -> int:
    """Inserta el batch ignorando duplicados. Devuelve el número insertado."""
    if not batch:
        return 0
    try:
        result = collection.insert_many(batch, ordered=False)
        return len(result.inserted_ids)
    except errors.BulkWriteError as bwe:
        # Algunos documentos ya existían — contar solo los insertados
        inserted = bwe.details.get("nInserted", 0)
        return inserted


if __name__ == "__main__":
    main()
