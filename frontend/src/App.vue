<template>
  <div class="min-h-screen bg-background">
    <header class="border-b">
      <div class="max-w-5xl mx-auto flex items-center gap-6 px-6 h-14">
        <router-link to="/" class="font-bold text-lg tracking-tight">
          ExamManager
        </router-link>
        <Separator orientation="vertical" class="h-6" />
        <nav class="flex items-center gap-1">
          <Button variant="ghost" size="sm" as-child>
            <router-link to="/admin/exams">Manage</router-link>
          </Button>
          <Button variant="ghost" size="sm" as-child>
            <router-link to="/admin/scores">Scores</router-link>
          </Button>
          <Button variant="ghost" size="sm" as-child>
            <router-link to="/exam/login">Exam</router-link>
          </Button>
        </nav>
      </div>
    </header>
    <main class="max-w-5xl mx-auto px-6 py-8">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
</script>
