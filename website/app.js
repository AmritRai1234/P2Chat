// P2Chat Interactive P2P Mesh Simulator
document.addEventListener('DOMContentLoaded', () => {
  const canvas = document.getElementById('meshCanvas');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  let width, height;

  function resize() {
    width = canvas.width = canvas.parentElement.clientWidth;
    height = canvas.height = canvas.parentElement.clientHeight;
  }
  window.addEventListener('resize', resize);
  resize();

  // Mesh Node Class
  class Node {
    constructor(x, y, isHost = false) {
      this.x = x || Math.random() * (width - 60) + 30;
      this.y = y || Math.random() * (height - 60) + 30;
      this.vx = (Math.random() - 0.5) * 0.8;
      this.vy = (Math.random() - 0.5) * 0.8;
      this.radius = isHost ? 8 : 5;
      this.pulse = Math.random() * Math.PI * 2;
      this.isHost = isHost;
    }

    update() {
      this.x += this.vx;
      this.y += this.vy;

      if (this.x < 20 || this.x > width - 20) this.vx *= -1;
      if (this.y < 20 || this.y > height - 20) this.vy *= -1;

      this.pulse += 0.05;
    }

    draw() {
      // Signal pulse ring
      const ringRadius = this.radius + Math.sin(this.pulse) * 6 + 6;
      ctx.beginPath();
      ctx.arc(this.x, this.y, ringRadius, 0, Math.PI * 2);
      ctx.strokeStyle = this.isHost ? 'rgba(255, 255, 255, 0.4)' : 'rgba(255, 255, 255, 0.15)';
      ctx.lineWidth = 1.2;
      ctx.stroke();

      // Core Node
      ctx.beginPath();
      ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
      ctx.fillStyle = this.isHost ? '#FFFFFF' : '#888888';
      ctx.shadowBlur = 8;
      ctx.shadowColor = '#FFFFFF';
      ctx.fill();
      ctx.shadowBlur = 0;
    }
  }

  // Create initial mesh nodes
  const nodes = [];
  nodes.push(new Node(width * 0.3, height * 0.5, true));
  nodes.push(new Node(width * 0.7, height * 0.4, false));
  for (let i = 0; i < 6; i++) {
    nodes.push(new Node());
  }

  // Active P2P Encrypted Packets
  const packets = [];
  function spawnPacket() {
    if (nodes.length < 2) return;
    const fromIdx = Math.floor(Math.random() * nodes.length);
    let toIdx = Math.floor(Math.random() * nodes.length);
    while (toIdx === fromIdx) {
      toIdx = Math.floor(Math.random() * nodes.length);
    }
    packets.push({
      from: nodes[fromIdx],
      to: nodes[toIdx],
      progress: 0,
      speed: 0.015 + Math.random() * 0.01
    });
  }

  setInterval(spawnPacket, 1200);

  // Click to add node
  canvas.addEventListener('click', (e) => {
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    nodes.push(new Node(x, y));
  });

  // Animation Loop
  function animate() {
    ctx.clearRect(0, 0, width, height);

    // Draw Mesh Edges
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const dx = nodes[i].x - nodes[j].x;
        const dy = nodes[i].y - nodes[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 160) {
          ctx.beginPath();
          ctx.moveTo(nodes[i].x, nodes[i].y);
          ctx.lineTo(nodes[j].x, nodes[j].y);
          const alpha = (1 - dist / 160) * 0.35;
          ctx.strokeStyle = `rgba(255, 255, 255, ${alpha})`;
          ctx.lineWidth = 1;
          ctx.stroke();
        }
      }
    }

    // Update and Draw Nodes
    nodes.forEach(node => {
      node.update();
      node.draw();
    });

    // Update and Draw Packets
    for (let i = packets.length - 1; i >= 0; i--) {
      const p = packets[i];
      p.progress += p.speed;

      if (p.progress >= 1) {
        packets.splice(i, 1);
        continue;
      }

      const px = p.from.x + (p.to.x - p.from.x) * p.progress;
      const py = p.from.y + (p.to.y - p.from.y) * p.progress;

      ctx.beginPath();
      ctx.arc(px, py, 2.5, 0, Math.PI * 2);
      ctx.fillStyle = '#FFFFFF';
      ctx.shadowBlur = 6;
      ctx.shadowColor = '#FFFFFF';
      ctx.fill();
      ctx.shadowBlur = 0;
    }

    requestAnimationFrame(animate);
  }

  animate();

  // Theme Toggle Switch Logic
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  const themeIcon = document.getElementById('themeIcon');

  function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('p2pchat_theme', theme);
    if (themeIcon) {
      themeIcon.setAttribute('data-lucide', theme === 'light' ? 'moon' : 'sun');
    }
    if (window.lucide) {
      lucide.createIcons();
    }
  }

  const savedTheme = localStorage.getItem('p2pchat_theme') || 'dark';
  setTheme(savedTheme);

  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      setTheme(newTheme);
    });
  }

  if (window.lucide) {
    lucide.createIcons();
  }
});
