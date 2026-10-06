-- =========================================================================
-- V1__init_schema.sql: Criação inicial das tabelas do Sistema de Barbearias
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- 1. Barbearias / Salões (Tenants)
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(30),
    email VARCHAR(150),
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);

-- 2. Usuários Administrativos / Operadores
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'TENANT_ADMIN', 'OPERATOR')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

-- 3. Profissionais (Barbeiros / Atendentes)
CREATE TABLE IF NOT EXISTS professionals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    bio TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_professionals_tenant_id ON professionals(tenant_id);

-- 4. Serviços Oferecidos
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_services_tenant_id ON services(tenant_id);

-- 5. Relação N:N Profissionais <-> Serviços
CREATE TABLE IF NOT EXISTS professional_services (
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (professional_id, service_id)
);

-- 6. Grade Semanal de Trabalho do Profissional
CREATE TABLE IF NOT EXISTS working_hours (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1=Segunda, 7=Domingo
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start TIME,
    break_end TIME,
    slot_duration_minutes SMALLINT NOT NULL DEFAULT 30,
    CONSTRAINT chk_time_window CHECK (start_time < end_time),
    CONSTRAINT chk_break_window CHECK (break_start IS NULL OR (break_start < break_end AND break_start >= start_time AND break_end <= end_time))
);

CREATE INDEX IF NOT EXISTS idx_working_hours_prof ON working_hours(professional_id, day_of_week);

-- 7. Bloqueios de Horários / Folgas / Pausas
CREATE TABLE IF NOT EXISTS schedule_blocks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE CASCADE,
    reason VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_block_window CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_blocks_prof_period ON schedule_blocks(professional_id, start_time, end_time);

-- 8. Agendamentos
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    professional_id UUID NOT NULL REFERENCES professionals(id) ON DELETE RESTRICT,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    customer_name VARCHAR(120) NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    cancellation_token VARCHAR(64) NOT NULL UNIQUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_appt_duration CHECK (start_time < end_time)
);

-- Constraint anti-conflito de horários (PostgreSQL Exclusion Constraint)
-- Garante a nível de storage que um profissional não tenha 2 agendamentos no mesmo horário
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'no_overlapping_appointments'
    ) THEN
        ALTER TABLE appointments
            ADD CONSTRAINT no_overlapping_appointments
            EXCLUDE USING gist (
                professional_id WITH =,
                tstzrange(start_time, end_time, '[)') WITH &&
            ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_appointments_tenant_date ON appointments(tenant_id, start_time);
CREATE INDEX IF NOT EXISTS idx_appointments_prof_date ON appointments(professional_id, start_time);
CREATE INDEX IF NOT EXISTS idx_appointments_token ON appointments(cancellation_token);

-- =========================================================================
-- DADOS SEED (Administrador padrão e Barbearia Modelo para testes)
-- Senha padrão para todos: "admin123" (hash bcrypt: $2a$12$e8YJ8f6YxM6qE7tW3A0KSu/d0eO6zL.aT5Ue7Mqv7x09.V5Z1bF.O)
-- =========================================================================
DO $$
DECLARE
    v_tenant_id UUID;
    v_prof_carlos UUID;
    v_prof_marcos UUID;
    v_service_corte UUID;
    v_service_barba UUID;
    v_service_combo UUID;
    v_hash VARCHAR(255) := '$2a$12$3ro2rbAQBWVS4H0vYt5TxuL.QsWq7A/PmPaq/eKJaXcdEchBS1VRa'; -- admin123
BEGIN
    -- 1. Super Admin
    IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'superadmin@barbershop.com') THEN
        INSERT INTO users (name, email, password_hash, role, active)
        VALUES ('Super Administrador', 'superadmin@barbershop.com', v_hash, 'SUPER_ADMIN', TRUE);
    END IF;

    -- 2. Tenant de Exemplo: Barbearia Vintage Club
    IF NOT EXISTS (SELECT 1 FROM tenants WHERE slug = 'barbearia-vintage') THEN
        INSERT INTO tenants (name, slug, phone, email, timezone, active)
        VALUES ('Barbearia Vintage Club', 'barbearia-vintage', '(11) 98888-7777', 'contato@vintageclub.com', 'America/Sao_Paulo', TRUE)
        RETURNING id INTO v_tenant_id;

        -- Admin do Tenant
        INSERT INTO users (tenant_id, name, email, password_hash, role, active)
        VALUES (v_tenant_id, 'Gerente Vintage', 'admin@vintageclub.com', v_hash, 'TENANT_ADMIN', TRUE);

        -- Operador do Tenant
        INSERT INTO users (tenant_id, name, email, password_hash, role, active)
        VALUES (v_tenant_id, 'Recepção Vintage', 'operador@vintageclub.com', v_hash, 'OPERATOR', TRUE);

        -- Profissionais
        INSERT INTO professionals (id, tenant_id, name, email, phone, bio, active)
        VALUES 
            (uuid_generate_v4(), v_tenant_id, 'Carlos Navalha', 'carlos@vintageclub.com', '(11) 99111-2233', 'Especialista em cortes clássicos e degradê navalhado há 10 anos.', TRUE)
            RETURNING id INTO v_prof_carlos;

        INSERT INTO professionals (id, tenant_id, name, email, phone, bio, active)
        VALUES 
            (uuid_generate_v4(), v_tenant_id, 'Marcos Barbeiro', 'marcos@vintageclub.com', '(11) 99222-3344', 'Mestre em barboterapia, toalha quente e alinhamento de barba.', TRUE)
            RETURNING id INTO v_prof_marcos;

        -- Serviços
        INSERT INTO services (id, tenant_id, name, description, duration_minutes, price, active)
        VALUES (uuid_generate_v4(), v_tenant_id, 'Corte Clássico & Degradê', 'Corte completo com tesoura e máquina, finalização com pomada modeladora.', 30, 45.00, TRUE)
        RETURNING id INTO v_service_corte;

        INSERT INTO services (id, tenant_id, name, description, duration_minutes, price, active)
        VALUES (uuid_generate_v4(), v_tenant_id, 'Barba & Barboterapia', 'Tratamento com toalha quente, óleos essenciais e navalha descartável.', 30, 35.00, TRUE)
        RETURNING id INTO v_service_barba;

        INSERT INTO services (id, tenant_id, name, description, duration_minutes, price, active)
        VALUES (uuid_generate_v4(), v_tenant_id, 'Combo Cabelo + Barba', 'Experiência completa de corte e barba com acabamento impecável.', 60, 70.00, TRUE)
        RETURNING id INTO v_service_combo;

        -- Vinculação Profissionais e Serviços
        INSERT INTO professional_services (professional_id, service_id) VALUES
            (v_prof_carlos, v_service_corte),
            (v_prof_carlos, v_service_combo),
            (v_prof_marcos, v_service_barba),
            (v_prof_marcos, v_service_combo),
            (v_prof_marcos, v_service_corte);

        -- Horários de Trabalho (Segunda a Sábado, 09:00 às 18:00 com pausa de almoço 12:00 às 13:00)
        -- Dias 1 (Seg) a 6 (Sáb)
        FOR day_idx IN 1..6 LOOP
            INSERT INTO working_hours (professional_id, day_of_week, start_time, end_time, break_start, break_end, slot_duration_minutes)
            VALUES 
                (v_prof_carlos, day_idx, '09:00:00', '18:00:00', '12:00:00', '13:00:00', 30),
                (v_prof_marcos, day_idx, '10:00:00', '19:00:00', '13:00:00', '14:00:00', 30);
        END LOOP;
    END IF;
END $$;
