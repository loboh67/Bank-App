--
-- PostgreSQL database dump
--

\restrict rR5TIyYBeVU50vaSLdif6XvKzOhsEBdXSIWoNEukulzHO13z0MZlJQJrGcK1gDZ

-- Dumped from database version 14.20 (Homebrew)
-- Dumped by pg_dump version 14.20 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: set_updated_at(); Type: FUNCTION; Schema: public; Owner: henriquelobo
--

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION public.set_updated_at() OWNER TO henriquelobo;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bank_accounts; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.bank_accounts (
    id bigint NOT NULL,
    user_id character varying(255) NOT NULL,
    provider_account_id character varying(255) NOT NULL,
    identification_hash character varying(255) NOT NULL,
    iban character varying(255),
    name character varying(255),
    status character varying(255),
    created_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    eb_continuation_key text
);


ALTER TABLE public.bank_accounts OWNER TO henriquelobo;

--
-- Name: accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: henriquelobo
--

CREATE SEQUENCE public.accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.accounts_id_seq OWNER TO henriquelobo;

--
-- Name: accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: henriquelobo
--

ALTER SEQUENCE public.accounts_id_seq OWNED BY public.bank_accounts.id;


--
-- Name: bank_sessions; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.bank_sessions (
    id bigint NOT NULL,
    user_id character varying(255) NOT NULL,
    valid_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT now(),
    state character varying(255),
    aspsp_name character varying(255) NOT NULL,
    aspsp_country character varying(255) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    status character varying(255) DEFAULT 'PENDING'::text NOT NULL
);


ALTER TABLE public.bank_sessions OWNER TO henriquelobo;

--
-- Name: bank_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: henriquelobo
--

CREATE SEQUENCE public.bank_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.bank_sessions_id_seq OWNER TO henriquelobo;

--
-- Name: bank_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: henriquelobo
--

ALTER SEQUENCE public.bank_sessions_id_seq OWNED BY public.bank_sessions.id;


--
-- Name: categories; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.categories (
    id bigint NOT NULL,
    key text NOT NULL,
    name text NOT NULL,
    parent_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.categories OWNER TO henriquelobo;

--
-- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: henriquelobo
--

CREATE SEQUENCE public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.categories_id_seq OWNER TO henriquelobo;

--
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: henriquelobo
--

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;


--
-- Name: merchants; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.merchants (
    id bigint NOT NULL,
    key text NOT NULL,
    name text NOT NULL,
    logo_url text,
    website text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.merchants OWNER TO henriquelobo;

--
-- Name: merchants_id_seq; Type: SEQUENCE; Schema: public; Owner: henriquelobo
--

CREATE SEQUENCE public.merchants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.merchants_id_seq OWNER TO henriquelobo;

--
-- Name: merchants_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: henriquelobo
--

ALTER SEQUENCE public.merchants_id_seq OWNED BY public.merchants.id;


--
-- Name: transaction_categories; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.transaction_categories (
    id bigint NOT NULL,
    transaction_id bigint NOT NULL,
    category_id bigint NOT NULL,
    confidence numeric(5,4),
    source text NOT NULL,
    is_primary boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.transaction_categories OWNER TO henriquelobo;

--
-- Name: transaction_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: henriquelobo
--

CREATE SEQUENCE public.transaction_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.transaction_categories_id_seq OWNER TO henriquelobo;

--
-- Name: transaction_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: henriquelobo
--

ALTER SEQUENCE public.transaction_categories_id_seq OWNED BY public.transaction_categories.id;


--
-- Name: transactions; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.transactions (
    id bigint NOT NULL,
    user_id character varying(255) NOT NULL,
    bank_account_id bigint NOT NULL,
    provider_transaction_id character varying(255) NOT NULL,
    amount numeric(38,2),
    currency character varying(255),
    direction character varying(255),
    booking_date date,
    value_date date,
    raw_json jsonb,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    description_raw text,
    description_display text,
    merchant_id bigint,
    description character varying(255)
);


ALTER TABLE public.transactions OWNER TO henriquelobo;

--
-- Name: transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: henriquelobo
--

CREATE SEQUENCE public.transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.transactions_id_seq OWNER TO henriquelobo;

--
-- Name: transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: henriquelobo
--

ALTER SEQUENCE public.transactions_id_seq OWNED BY public.transactions.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: henriquelobo
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.users OWNER TO henriquelobo;

--
-- Name: bank_accounts id; Type: DEFAULT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_accounts ALTER COLUMN id SET DEFAULT nextval('public.accounts_id_seq'::regclass);


--
-- Name: bank_sessions id; Type: DEFAULT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_sessions ALTER COLUMN id SET DEFAULT nextval('public.bank_sessions_id_seq'::regclass);


--
-- Name: categories id; Type: DEFAULT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);


--
-- Name: merchants id; Type: DEFAULT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.merchants ALTER COLUMN id SET DEFAULT nextval('public.merchants_id_seq'::regclass);


--
-- Name: transaction_categories id; Type: DEFAULT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transaction_categories ALTER COLUMN id SET DEFAULT nextval('public.transaction_categories_id_seq'::regclass);


--
-- Name: transactions id; Type: DEFAULT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transactions ALTER COLUMN id SET DEFAULT nextval('public.transactions_id_seq'::regclass);


--
-- Name: bank_accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: bank_accounts accounts_user_id_provider_account_id_key; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT accounts_user_id_provider_account_id_key UNIQUE (user_id, provider_account_id);


--
-- Name: bank_accounts bank_accounts_user_iban_unique; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_user_iban_unique UNIQUE (user_id, iban);


--
-- Name: bank_sessions bank_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_sessions
    ADD CONSTRAINT bank_sessions_pkey PRIMARY KEY (id);


--
-- Name: bank_sessions bank_sessions_user_aspsp_unique; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_sessions
    ADD CONSTRAINT bank_sessions_user_aspsp_unique UNIQUE (user_id, aspsp_name, aspsp_country);


--
-- Name: categories categories_key_key; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_key_key UNIQUE (key);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: merchants merchants_key_key; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_key_key UNIQUE (key);


--
-- Name: merchants merchants_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_pkey PRIMARY KEY (id);


--
-- Name: transaction_categories transaction_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transaction_categories
    ADD CONSTRAINT transaction_categories_pkey PRIMARY KEY (id);


--
-- Name: transaction_categories transaction_categories_transaction_id_unique; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transaction_categories
    ADD CONSTRAINT transaction_categories_transaction_id_unique UNIQUE (transaction_id);


--
-- Name: transactions transactions_bank_account_id_provider_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_bank_account_id_provider_transaction_id_key UNIQUE (bank_account_id, provider_transaction_id);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: bank_accounts ukcw3gaqdap2pcxljn26rsjcm1w; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT ukcw3gaqdap2pcxljn26rsjcm1w UNIQUE (user_id, provider_account_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: transaction_categories ux_transaction_categories_tx_cat; Type: CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transaction_categories
    ADD CONSTRAINT ux_transaction_categories_tx_cat UNIQUE (transaction_id, category_id);


--
-- Name: idx_bank_sessions_user_aspsp; Type: INDEX; Schema: public; Owner: henriquelobo
--

CREATE INDEX idx_bank_sessions_user_aspsp ON public.bank_sessions USING btree (user_id, aspsp_name, aspsp_country);


--
-- Name: transactions_merchant_id_idx; Type: INDEX; Schema: public; Owner: henriquelobo
--

CREATE INDEX transactions_merchant_id_idx ON public.transactions USING btree (merchant_id);


--
-- Name: ux_transaction_categories_primary; Type: INDEX; Schema: public; Owner: henriquelobo
--

CREATE UNIQUE INDEX ux_transaction_categories_primary ON public.transaction_categories USING btree (transaction_id) WHERE (is_primary = true);


--
-- Name: users trg_users_set_updated_at; Type: TRIGGER; Schema: public; Owner: henriquelobo
--

CREATE TRIGGER trg_users_set_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();


--
-- Name: categories categories_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.categories(id);


--
-- Name: transaction_categories transaction_categories_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transaction_categories
    ADD CONSTRAINT transaction_categories_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id);


--
-- Name: transaction_categories transaction_categories_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transaction_categories
    ADD CONSTRAINT transaction_categories_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.transactions(id) ON DELETE CASCADE;


--
-- Name: transactions transactions_bank_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_bank_account_id_fkey FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id);


--
-- Name: transactions transactions_merchant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: henriquelobo
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_merchant_id_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(id);


--
-- PostgreSQL database dump complete
--

\unrestrict rR5TIyYBeVU50vaSLdif6XvKzOhsEBdXSIWoNEukulzHO13z0MZlJQJrGcK1gDZ

