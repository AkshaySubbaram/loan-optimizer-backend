# 🚀 Loan Strategy Calculator - Backend (Spring Boot + Kubernetes + Argo CD)

A production-style backend application demonstrating **CI/CD, Docker, Kubernetes, Ingress, and GitOps (Argo CD)**.

---

# 🧠 Architecture Overview

```
Browser
   ↓
Ingress (NGINX)
   ↓
Kubernetes Service (ClusterIP)
   ↓
Pods (Spring Boot App - Port 9898)
```

CI/CD Flow:

```
Code → GitHub → CI → Docker Image → Argo CD → Kubernetes → Live App
```

---

# 📦 Tech Stack

* Java 17
* Spring Boot
* Docker
* Kubernetes (Docker Desktop / Minikube)
* Argo CD (GitOps)
* NGINX Ingress Controller
* GitHub Actions (CI/CD)

---

# ⚙️ Prerequisites

Make sure the following are installed:

* Docker Desktop (with Kubernetes enabled)
* kubectl CLI
* Git
* Java 17 (for local dev, optional)

---

# 🚀 Local Setup Guide

## 1️⃣ Clone Repository

```
git clone https://github.com/AkshaySubbaram/loan-optimizer-backend.git
cd loan-optimizer-backend
```

---

## 2️⃣ Enable Kubernetes

* Open Docker Desktop
* Go to Settings → Kubernetes
* Enable Kubernetes
* Wait until it shows **Running**

Verify:

```
kubectl get nodes
```

---

## 3️⃣ Apply Kubernetes Manifests

```
kubectl apply -f k8s/
```

Verify:

```
kubectl get pods
kubectl get svc
```

---

## 4️⃣ Setup Local Domain (Hosts File)

Open as Administrator:

```
C:\Windows\System32\drivers\etc\hosts
```

Add:

```
127.0.0.1 loan-strategy-calculator.local
```

Flush DNS:

```
ipconfig /flushdns
```

---

## 5️⃣ Access Application

Open in browser:

```
http://loan-strategy-calculator.local/loan/summary
```

---

# 🔁 CI/CD Pipeline

## ✅ CI (Continuous Integration)

Triggered on:

* push to main/master
* pull requests

Steps:

* Build project
* Run tests
* Upload test reports

---

## ✅ CD (Continuous Deployment)

Triggered on:

* push to main/master

Steps:

* Build JAR
* Build Docker image
* Tag image using commit SHA
* Push to Docker Hub

Example:

```
docker build -t <username>/loan-backend:<commit-id>
docker push <username>/loan-backend:<commit-id>
```

---

# 🔄 GitOps with Argo CD

Argo CD watches this repository and:

* Detects changes in `k8s/`
* Automatically syncs with Kubernetes
* Applies updates without manual `kubectl`

### Features enabled:

* Auto Sync
* Self Healing
* Auto Prune

---

# 🌐 Ingress Configuration

Ingress provides a clean URL:

```
http://loan-strategy-calculator.local
```

Instead of:

```
localhost:30007 ❌
localhost:8089 ❌
```

---

# 📁 Project Structure

```
loan-optimizer-backend/
│
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
│
├── scripts/
│   └── start-system.ps1
│
├── .github/workflows/
│   ├── ci.yml
│   └── cd.yml
│
├── src/
├── pom.xml
└── README.md
```

---

# 🔧 Useful Commands

### Check pods

```
kubectl get pods
```

### Check logs

```
kubectl logs <pod-name>
```

### Check ingress

```
kubectl get ingress
```

### Port forward (for debugging)

```
kubectl port-forward svc/loan-backend-service 8089:80
```

---

# 🐞 Troubleshooting

## ❌ App not accessible via domain

* Check hosts file entry
* Run `ipconfig /flushdns`
* Verify ingress:

```
kubectl get ingress
```

---

## ❌ Pods crashing

```
kubectl logs <pod-name>
```

---

## ❌ Service not routing

```
kubectl describe svc loan-backend-service
```

---

## ❌ Ingress not working

* Ensure:

```
ingressClassName: nginx
```

* Check controller:

```
kubectl get pods -n ingress-nginx
```

---

# 🧠 Key Concepts Demonstrated

* Containerization using Docker
* Kubernetes deployment & scaling
* Service abstraction (ClusterIP)
* Ingress-based routing
* GitOps using Argo CD
* CI/CD automation using GitHub Actions

---

# 🚀 Future Enhancements

* Add multiple microservices (user, auth)
* Implement API Gateway pattern via Ingress
* Deploy to Azure Kubernetes Service (AKS)
* Add HTTPS using cert-manager
* Integrate monitoring (Prometheus + Grafana)

---

# 👨‍💻 Author

Akshay S

---

# ⭐ Final Note

This project is designed to simulate a **real-world production backend system** locally, including DevOps practices used in modern cloud-native applications.
